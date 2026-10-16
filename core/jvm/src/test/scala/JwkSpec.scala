package pdi.jwt

import java.math.BigInteger
import java.security.spec.{ECGenParameterSpec, InvalidKeySpecException}
import java.security.{KeyPairGenerator, MessageDigest, PublicKey}

import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.exceptions.*

class JwkSpec extends munit.FunSuite {
  import JwkTestKeys.*

  private val encoder = java.util.Base64.getUrlEncoder.withoutPadding

  private def independentThumbprint(json: String): String =
    encoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(json.getBytes("UTF-8")))

  private def b64(value: BigInteger, length: Int): String = {
    val bytes = unsignedBytes(value)
    encoder.encodeToString(new Array[Byte](length - bytes.length) ++ bytes)
  }

  private def rfcRsa(): RsaJwk = RsaJwk(
    None,
    None,
    None,
    new BigInteger(1, JwtBase64.decode(Rfc7638Modulus)),
    new BigInteger(1, JwtBase64.decode("AQAB"))
  )

  test("fromPublicKey should build an RsaJwk that gives back the key") {
    val key = rsaPublic(rsa())
    val jwk = Jwk.fromPublicKey(key)
    assertEquals(jwk, RsaJwk(None, None, None, key.getModulus, key.getPublicExponent))
    assertEquals(jwk.publicKey, key)
  }

  EcCurve.all().foreach { curve =>
    test(s"fromPublicKey should build an EcJwk on ${curve.name} that gives back the key") {
      val key = ecPublic(ec(curve))
      val jwk = Jwk.fromPublicKey(key)
      assertEquals(jwk, EcJwk(None, None, None, curve, key.getW.getAffineX, key.getW.getAffineY))
      assertEquals(jwk.publicKey, key)
    }
  }

  OkpCurve.all().foreach { curve =>
    test(s"fromPublicKey should build an OkpJwk on ${curve.name} that gives back the key") {
      val key = okp(curve).getPublic
      val jwk = Jwk.fromPublicKey(key)
      assertEquals(jwk.asInstanceOf[OkpJwk].curve, curve)
      assertEquals(jwk.asInstanceOf[OkpJwk].keyBytes.length, curve.keyLength)
      assertEquals(jwk.publicKey, key)
    }
  }

  test("fromPublicKey should keep the kid, use and alg") {
    val jwk = Jwk.fromPublicKey(
      rsaPublic(rsa()),
      Some("k1"),
      Some(JwkUse.Signature),
      Some("RS256")
    )
    assertEquals(jwk.keyId, Some("k1"))
    assertEquals(jwk.use, Some(JwkUse.Signature))
    assertEquals(jwk.algorithm, Some("RS256"))
  }

  test("fromPublicKey should reject an X25519 key") {
    val generator = KeyPairGenerator.getInstance("X25519")
    intercept[JwtNonSupportedAlgorithm] {
      Jwk.fromPublicKey(generator.generateKeyPair.getPublic)
    }
  }

  test("fromPublicKey should reject a DSA key") {
    val dsa = KeyPairGenerator.getInstance("DSA")
    dsa.initialize(2048)
    intercept[JwtNonSupportedAlgorithm] {
      Jwk.fromPublicKey(dsa.generateKeyPair.getPublic)
    }
  }

  test("fromPublicKey should reject an EC curve that is not supported") {
    val generator = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider())
    generator.initialize(new ECGenParameterSpec("secp256k1"))
    val key: PublicKey = generator.generateKeyPair.getPublic
    intercept[JwtNonSupportedCurve] {
      Jwk.fromPublicKey(key)
    }
  }

  test("toJson should write kty, kid, use, alg then the key parameters") {
    val key = rsaPublic(rsa())
    val jwk = Jwk.fromPublicKey(key, Some("k1"), Some(JwkUse.Signature), Some("RS256"))
    assertEquals(
      jwk.toJson,
      "{\"kty\":\"RSA\",\"kid\":\"k1\",\"use\":\"sig\",\"alg\":\"RS256\"," +
        "\"n\":\"" + encoder.encodeToString(unsignedBytes(key.getModulus)) + "\"," +
        "\"e\":\"AQAB\"}"
    )
  }

  test("toJson should omit absent optional members") {
    assert(rfcRsa().toJson.startsWith("{\"kty\":\"RSA\",\"n\":"), rfcRsa().toJson)
  }

  test("toJson should write the use values") {
    assert(
      Jwk
        .fromPublicKey(rsaPublic(rsa()), use = Some(JwkUse.Encryption))
        .toJson
        .contains("\"use\":\"enc\"")
    )
    assert(
      Jwk
        .fromPublicKey(rsaPublic(rsa()), use = Some(JwkUse.Other("x")))
        .toJson
        .contains("\"use\":\"x\"")
    )
  }

  test("toJson should write an RSA modulus with its top bit set without a sign byte") {
    val jwk = Jwk.fromPublicKey(rsaPublic(rsa()))
    val modulus = JwtBase64.decode(jwk.asInstanceOf[RsaJwk].n)
    assertEquals(modulus.length, 256)
    assert((modulus(0) & 0x80) != 0)
  }

  test("toJson should write EC coordinates with their fixed length") {
    val jwk = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256))).asInstanceOf[EcJwk]
    assertEquals(JwtBase64.decode(jwk.x).length, 32)
    assertEquals(JwtBase64.decode(jwk.y).length, 32)
    assert(jwk.toJson.contains("\"x\":\"" + jwk.x + "\""))
    assert(jwk.toJson.contains("\"y\":\"" + jwk.y + "\""))
  }

  test("toJson should pad generated EC coordinates that have leading zero bytes") {
    val short = Iterator
      .continually(ecPublic(ec(EcCurve.P521)))
      .take(500)
      .find(k => unsignedBytes(k.getW.getAffineX).length < 66)
    assert(short.isDefined)
    val key = short.get
    val jwk = Jwk.fromPublicKey(key).asInstanceOf[EcJwk]
    assertEquals(jwk.x.length, 88)
    assertEquals(jwk.y.length, 88)
    assertEquals(JwtBase64.decode(jwk.x).length, 66)
    assertEquals(JwtBase64.decode(jwk.y).length, 66)
    assertEquals(Jwk.fromPublicKey(key).publicKey, key)
  }

  test("EcJwk should reject a coordinate that does not fit the curve") {
    intercept[JwtValidationException] {
      EcJwk(None, None, None, EcCurve.P256, BigInteger.ONE.shiftLeft(256), BigInteger.ONE)
    }
  }

  test("EcJwk should reject a negative coordinate") {
    intercept[JwtValidationException] {
      EcJwk(None, None, None, EcCurve.P256, BigInteger.ONE, BigInteger.valueOf(-1))
    }
  }

  test("EcJwk should reject a point that is not on the curve") {
    val key = ecPublic(ec(EcCurve.P256))
    intercept[JwtValidationException] {
      EcJwk(
        None,
        None,
        None,
        EcCurve.P256,
        key.getW.getAffineX,
        key.getW.getAffineY.add(BigInteger.ONE)
      )
    }
  }

  test("RsaJwk.publicKey should let the exception of the JDK key factory propagate") {
    val jwk = RsaJwk(None, None, None, BigInteger.valueOf(65537), BigInteger.valueOf(65537))
    intercept[InvalidKeySpecException] {
      jwk.publicKey
    }
  }

  private def encode(bytes: Array[Byte]): String = JwtBase64.encodeString(bytes)

  test("OkpJwk should reject an Ed25519 key of the wrong length") {
    intercept[JwtValidationException] {
      OkpJwk(None, None, None, OkpCurve.Ed25519, encode(new Array[Byte](31)))
    }
  }

  test("OkpJwk should reject an Ed448 key of the wrong length") {
    intercept[JwtValidationException] {
      OkpJwk(None, None, None, OkpCurve.Ed448, encode(new Array[Byte](32)))
    }
  }

  test("OkpJwk should reject an empty key") {
    intercept[JwtValidationException] {
      OkpJwk(None, None, None, OkpCurve.Ed25519, "")
    }
  }

  test("OkpJwk should let the exception for a key that is not base64url propagate") {
    intercept[IllegalArgumentException] {
      OkpJwk(None, None, None, OkpCurve.Ed25519, "not base64url!!")
    }
  }

  test("OkpJwk should compare its key by value") {
    val a = OkpJwk(Some("k"), None, None, OkpCurve.Ed25519, encode(new Array[Byte](32)))
    val b = OkpJwk(Some("k"), None, None, OkpCurve.Ed25519, encode(new Array[Byte](32)))
    assertEquals(a, b)
    assertEquals(a.hashCode, b.hashCode)
    val other = new Array[Byte](32)
    other(0) = 1
    assertNotEquals(a, a.copy(x = encode(other)))
    assertNotEquals(a, a.copy(keyId = None))
  }

  test("OkpJwk should keep its key as the base64url member it was built from") {
    val jwk = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)
    assertEquals(jwk.x, Rfc8037Ed25519X)
    assertEquals(jwk.keyBytes.toList, JwtBase64.decode(Rfc8037Ed25519X).toList)
    assertEquals(jwk, OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X))
  }

  test("OkpJwk should not be changed by the array its accessor returns") {
    val jwk = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)
    val published = jwk.publicKey
    val exposed = jwk.keyBytes
    assertNotEquals(exposed: AnyRef, jwk.keyBytes: AnyRef)
    exposed(0) = (exposed(0) ^ 1).toByte
    assertEquals(jwk.keyBytes.toList, JwtBase64.decode(Rfc8037Ed25519X).toList)
    assertEquals(jwk.x, Rfc8037Ed25519X)
    assertEquals(jwk.thumbprint.base64Url, Rfc8037Ed25519Thumbprint)
    assertEquals(jwk.publicKey, published)
  }

  test("OkpJwk.copy should validate the key it is given and keep the other members") {
    val jwk = OkpJwk(Some("k"), None, None, OkpCurve.Ed25519, encode(new Array[Byte](32)))
    val bytes = new Array[Byte](32)
    bytes(0) = 1
    val copy = jwk.copy(keyId = Some("other"), x = encode(bytes))
    assertEquals(copy.keyId, Some("other"))
    assertEquals(copy.keyBytes(0), 1.toByte)
    assertEquals(jwk.copy(), jwk)
    assertEquals(jwk.keyBytes.toList, List.fill(32)(0.toByte))
    intercept[JwtValidationException] {
      jwk.copy(x = encode(new Array[Byte](31)))
    }
  }

  test("OkpJwk.unapply should extract the members") {
    val x = encode(new Array[Byte](32))
    val jwk = OkpJwk(
      Some("k"),
      Some(JwkUse.Signature),
      None,
      OkpCurve.Ed25519,
      x,
      Some(Set("verify"))
    )
    jwk match {
      case OkpJwk(keyId, use, algorithm, curve, member, keyOperations) =>
        assertEquals(
          (keyId, use, algorithm, curve, member, keyOperations),
          (Some("k"), Some(JwkUse.Signature), None, OkpCurve.Ed25519, x, Some(Set("verify")))
        )
    }
  }

  test("keyOperations should default to None and be kept by fromPublicKey") {
    assertEquals(rfcRsa().keyOperations, None)
    assertEquals(Jwk.fromPublicKey(rsaPublic(rsa())).keyOperations, None)
    val ops = Some(Set("verify", "encrypt"))
    val keys: Seq[java.security.PublicKey] =
      Seq(rsaPublic(rsa()), ecPublic(ec(EcCurve.P256)), okp(OkpCurve.Ed25519).getPublic)
    keys.foreach { key =>
      assertEquals(Jwk.fromPublicKey(key, keyOperations = ops).keyOperations, ops)
    }
  }

  test("toJson should write key_ops as a sorted array after alg") {
    val key = rsaPublic(rsa())
    val jwk = Jwk.fromPublicKey(
      key,
      Some("k1"),
      Some(JwkUse.Signature),
      Some("RS256"),
      Some(Set("verify", "sign"))
    )
    assert(
      jwk.toJson.startsWith(
        "{\"kty\":\"RSA\",\"kid\":\"k1\",\"use\":\"sig\",\"alg\":\"RS256\"," +
          "\"key_ops\":[\"sign\",\"verify\"],\"n\":"
      ),
      jwk.toJson
    )
    assert(rfcRsa().toJson.startsWith("{\"kty\":\"RSA\",\"n\":"), rfcRsa().toJson)
    assert(rfcRsa().copy(keyOperations = Some(Set.empty)).toJson.contains("\"key_ops\":[],"))
  }

  test("thumbprint should ignore key_ops") {
    val jwk = rfcRsa().copy(keyOperations = Some(Set("verify")))
    assertEquals(jwk.thumbprint.base64Url, Rfc7638Thumbprint)
    val okpJwk = OkpJwk(
      None,
      None,
      None,
      OkpCurve.Ed25519,
      Rfc8037Ed25519X,
      Some(Set("verify"))
    )
    assertEquals(okpJwk.thumbprint.base64Url, Rfc8037Ed25519Thumbprint)
  }

  test("keyOperations should take part in equality") {
    val a = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X, Some(Set("verify")))
    val b = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X, Some(Set("verify")))
    assertEquals(a, b)
    assertEquals(a.hashCode, b.hashCode)
    assertNotEquals(a, a.copy(keyOperations = None))
    assertNotEquals(a, a.copy(keyOperations = Some(Set("sign"))))
    assertNotEquals(rfcRsa(), rfcRsa().copy(keyOperations = Some(Set("verify"))))
  }

  test("thumbprint should match the RFC 7638 section 3.1 example") {
    assertEquals(rfcRsa().thumbprint.base64Url, Rfc7638Thumbprint)
    assertEquals(JwkThumbprint.sha256(rfcRsa()).base64Url, Rfc7638Thumbprint)
  }

  test("thumbprint should ignore kid, use and alg") {
    val jwk =
      rfcRsa().copy(keyId = Some("k"), use = Some(JwkUse.Signature), algorithm = Some("RS256"))
    assertEquals(jwk.thumbprint.base64Url, Rfc7638Thumbprint)
  }

  test("thumbprint should match the RFC 8037 appendix A.3 Ed25519 example") {
    val jwk = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)
    assertEquals(jwk.thumbprint.base64Url, Rfc8037Ed25519Thumbprint)
    // JDK versions differ on PublicKey.getAlgorithm ("EdDSA" vs "Ed25519")
    assertEquals(Jwk.fromPublicKey(jwk.publicKey).asInstanceOf[OkpJwk].x, Rfc8037Ed25519X)
  }

  EcCurve.all().foreach { curve =>
    test(s"thumbprint should use the canonical JSON of an EC ${curve.name} key") {
      val key = ecPublic(ec(curve))
      val length = curve.coordinateLength
      val json = "{\"crv\":\"" + curve.name + "\",\"kty\":\"EC\",\"x\":\"" +
        b64(key.getW.getAffineX, length) + "\",\"y\":\"" + b64(key.getW.getAffineY, length) + "\"}"
      assertEquals(Jwk.fromPublicKey(key).thumbprint.base64Url, independentThumbprint(json))
      assertEquals(JwkThumbprint.sha256(key: PublicKey).base64Url, independentThumbprint(json))
    }
  }

  test("thumbprint of an EC key should pad short coordinates to the curve length") {
    val short = Iterator
      .continually(ecPublic(ec(EcCurve.P384)))
      .take(500)
      .find(k => unsignedBytes(k.getW.getAffineX).length < 48)
    assert(short.isDefined)
    val key = short.get
    val jwk = Jwk.fromPublicKey(key).asInstanceOf[EcJwk]
    val json = "{\"crv\":\"P-384\",\"kty\":\"EC\",\"x\":\"" + jwk.x + "\",\"y\":\"" + jwk.y + "\"}"
    assertEquals(jwk.thumbprint.base64Url, independentThumbprint(json))
    assertEquals(JwtBase64.decode(jwk.x).length, 48)
  }

  OkpCurve.all().foreach { curve =>
    test(s"thumbprint should use the canonical JSON of an OKP ${curve.name} key") {
      val key = okp(curve).getPublic
      val jwk = Jwk.fromPublicKey(key).asInstanceOf[OkpJwk]
      val json = "{\"crv\":\"" + curve.name + "\",\"kty\":\"OKP\",\"x\":\"" +
        jwk.x + "\"}"
      assertEquals(jwk.thumbprint.base64Url, independentThumbprint(json))
    }
  }

  test("JwkSet.find should return the first key with the kid") {
    val first = Jwk.fromPublicKey(rsaPublic(rsa()), Some("a"))
    val second = Jwk.fromPublicKey(rsaPublic(rsa()), Some("a"))
    val third = Jwk.fromPublicKey(rsaPublic(rsa()), Some("b"))
    val set = JwkSet(Seq(first, second, third))
    assertEquals(set.find("a"), Some(first))
    assertEquals(set.find("b"), Some(third))
    assertEquals(set.find("c"), None)
  }

  test("JwkSet.findAll should return every key with the kid in order") {
    val first = Jwk.fromPublicKey(rsaPublic(rsa()), Some("a"))
    val second = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256)), Some("a"))
    val third = Jwk.fromPublicKey(rsaPublic(rsa()), Some("b"))
    val noKid = Jwk.fromPublicKey(rsaPublic(rsa()))
    val set = JwkSet(Seq(first, third, noKid, second))
    assertEquals(set.findAll("a"), List(first, second))
    assertEquals(set.findAll("b"), List(third))
    assertEquals(set.findAll("c"), Nil)
    assertEquals(set.findAll(""), Nil)
  }

  test("JwkSet.find should never match a key without a kid") {
    val set = JwkSet(Seq(Jwk.fromPublicKey(rsaPublic(rsa()))))
    assertEquals(set.find(""), None)
    assertEquals(set.find("null"), None)
  }

  test("JwkSet.toJson should write the keys array") {
    assertEquals(JwkSet(Seq.empty).toJson, "{\"keys\":[]}")
    val a = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256)), Some("a"))
    val b = Jwk.fromPublicKey(okp(OkpCurve.Ed25519).getPublic, Some("b"))
    assertEquals(JwkSet(Seq(a, b)).toJson, "{\"keys\":[" + a.toJson + "," + b.toJson + "]}")
  }

  test("EcCurve and OkpCurve should be found by their JOSE name") {
    assertEquals(EcCurve.fromName("P-521"), Some(EcCurve.P521))
    assertEquals(EcCurve.fromName("secp256r1"), None)
    assertEquals(OkpCurve.fromName("Ed448"), Some(OkpCurve.Ed448))
    assertEquals(OkpCurve.fromName("X25519"), None)
  }

  test("RsaJwk should expose n and e as in the RFC 7638 section 3.1 example") {
    val jwk = rfcRsa()
    assertEquals(jwk.n, Rfc7638Modulus)
    assertEquals(jwk.e, "AQAB")
  }

  test("RsaJwk should expose a modulus with its top bit set without a sign byte") {
    val modulus = BigInteger.ONE.shiftLeft(2047).add(BigInteger.ONE)
    assertEquals(modulus.toByteArray.length, 257)
    val jwk = RsaJwk(None, None, None, modulus, BigInteger.valueOf(65537))
    val bytes = JwtBase64.decode(jwk.n)
    assertEquals(bytes.length, 256)
    assertEquals(bytes(0) & 0xff, 0x80)
    assertEquals(jwk.n.length, 342)
    assertEquals(jwk.e, "AQAB")
  }

  test("RsaJwk should expose a small exponent without leading zeros") {
    val jwk = RsaJwk(None, None, None, BigInteger.ONE.shiftLeft(2047), BigInteger.valueOf(3))
    assertEquals(jwk.e, "Aw")
  }

  test("EcJwk should expose P-521 coordinates of 66 bytes, 88 base64url characters") {
    val short = Iterator
      .continually(ecPublic(ec(EcCurve.P521)))
      .take(500)
      .find(k => unsignedBytes(k.getW.getAffineX).length < 66)
    assert(short.isDefined)
    val jwk = Jwk.fromPublicKey(short.get).asInstanceOf[EcJwk]
    assertEquals(jwk.x.length, 88)
    assertEquals(jwk.y.length, 88)
    assertEquals(JwtBase64.decode(jwk.x).length, 66)
    assertEquals(JwtBase64.decode(jwk.y).length, 66)
  }

  EcCurve.all().foreach { curve =>
    test(s"EcJwk should expose ${curve.name} coordinates of the fixed length") {
      val jwk = Jwk.fromPublicKey(ecPublic(ec(curve))).asInstanceOf[EcJwk]
      assertEquals(JwtBase64.decode(jwk.x).length, curve.coordinateLength)
      assertEquals(JwtBase64.decode(jwk.y).length, curve.coordinateLength)
    }
  }

  test("OkpJwk should expose x as in the RFC 8037 appendix A.3 example") {
    val jwk = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)
    assertEquals(jwk.x, Rfc8037Ed25519X)
  }

  test("the exposed strings should be the members written by toJson") {
    val rsaJwk = Jwk.fromPublicKey(rsaPublic(rsa())).asInstanceOf[RsaJwk]
    assert(rsaJwk.toJson.contains("\"n\":\"" + rsaJwk.n + "\",\"e\":\"" + rsaJwk.e + "\""))
    EcCurve.all().foreach { curve =>
      val ecJwk = Jwk.fromPublicKey(ecPublic(ec(curve))).asInstanceOf[EcJwk]
      assert(
        ecJwk.toJson.endsWith(
          "\"crv\":\"" + curve.name + "\",\"x\":\"" + ecJwk.x + "\",\"y\":\"" + ecJwk.y + "\"}"
        ),
        ecJwk.toJson
      )
    }
    OkpCurve.all().foreach { curve =>
      val okpJwk = Jwk.fromPublicKey(okp(curve).getPublic).asInstanceOf[OkpJwk]
      assert(
        okpJwk.toJson.endsWith("\"crv\":\"" + curve.name + "\",\"x\":\"" + okpJwk.x + "\"}"),
        okpJwk.toJson
      )
    }
  }

  test("the exposed strings should be the members of the thumbprint") {
    val ecJwk = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256))).asInstanceOf[EcJwk]
    val json =
      "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"" + ecJwk.x + "\",\"y\":\"" + ecJwk.y + "\"}"
    assertEquals(ecJwk.thumbprint.base64Url, independentThumbprint(json))
    val rsaJwk = rfcRsa()
    assertEquals(
      rsaJwk.thumbprint.base64Url,
      independentThumbprint(
        "{\"e\":\"" + rsaJwk.e + "\",\"kty\":\"RSA\",\"n\":\"" + rsaJwk.n + "\"}"
      )
    )
  }
}
