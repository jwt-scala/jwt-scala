package pdi.jwt

import java.math.BigInteger
import java.security.spec.InvalidKeySpecException

import io.circe.ParsingFailure
import pdi.jwt.exceptions.*

class JwtCirceJwkSpec extends munit.FunSuite {
  import JwkTestKeys.*

  private val encoder = java.util.Base64.getUrlEncoder.withoutPadding

  private def b64(bytes: Array[Byte]): String = encoder.encodeToString(bytes)

  private def rsaJson(kid: String, n: String, extra: String = ""): String =
    "{\"kty\":\"RSA\",\"kid\":\"" + kid + "\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + n +
      "\",\"e\":\"AQAB\"" + extra + "}"

  private def ecJson(curve: EcCurve, x: BigInteger, y: BigInteger): String =
    "{\"kty\":\"EC\",\"crv\":\"" + curve.name + "\",\"x\":\"" +
      JwtUtils.encodeFixed(x, curve.coordinateLength) + "\",\"y\":\"" +
      JwtUtils.encodeFixed(y, curve.coordinateLength) + "\"}"

  private def parseJwk(json: String): Jwk = JwtCirce.parseJwk(json)

  test("parseJwk should round trip every supported key type") {
    val keys: Seq[java.security.PublicKey] =
      Seq(rsaPublic(rsa())) ++ EcCurve.all().map(c => ecPublic(ec(c))) ++
        OkpCurve.all().map(c => okp(c).getPublic)
    keys.foreach { key =>
      val jwk = Jwk.fromPublicKey(key, Some("k"), Some(JwkUse.Signature), Some("ALG"))
      val parsed = parseJwk(jwk.toJson)
      assertEquals(parsed, jwk)
      assertEquals(parsed.publicKey, key)
      assertEquals(parsed.toJson, jwk.toJson)
      assertEquals(parsed.thumbprint, jwk.thumbprint)
    }
  }

  test("parseJwk should round trip an EC key with a short coordinate") {
    val key = Iterator
      .continually(ecPublic(ec(EcCurve.P521)))
      .take(500)
      .find(k => unsignedBytes(k.getW.getAffineY).length < 66)
      .get
    val jwk = Jwk.fromPublicKey(key)
    assertEquals(parseJwk(jwk.toJson).publicKey, key)
  }

  test("parseJwk should read the use member") {
    def use(value: String) = parseJwk(rsaJson("k", Rfc7638Modulus).replace("\"sig\"", value)).use
    assertEquals(use("\"sig\""), Some(JwkUse.Signature))
    assertEquals(use("\"enc\""), Some(JwkUse.Encryption))
    assertEquals(use("\"other\""), Some(JwkUse.Other("other")))
  }

  test("parseJwk should read the key_ops member") {
    def ops(value: String) =
      parseJwk(rsaJson("k", Rfc7638Modulus, ",\"key_ops\":" + value)).keyOperations
    assertEquals(ops("[\"verify\"]"), Some(Set("verify")))
    assertEquals(ops("[\"verify\",\"encrypt\",\"verify\"]"), Some(Set("verify", "encrypt")))
    assertEquals(ops("[]"), Some(Set.empty[String]))
    assertEquals(parseJwk(rsaJson("k", Rfc7638Modulus)).keyOperations, None)
  }

  test("parseJwk should read key_ops on every key type and write it back") {
    val keys: Seq[java.security.PublicKey] =
      Seq(rsaPublic(rsa()), ecPublic(ec(EcCurve.P384)), okp(OkpCurve.Ed448).getPublic)
    keys.foreach { key =>
      val jwk = Jwk.fromPublicKey(key, Some("k"), keyOperations = Some(Set("verify", "sign")))
      val parsed = parseJwk(jwk.toJson)
      assertEquals(parsed, jwk)
      assertEquals(parsed.keyOperations, Some(Set("verify", "sign")))
      assertEquals(parsed.toJson, jwk.toJson)
    }
  }

  test("parseJwk should reject a key_ops that is not an array of strings") {
    Seq("\"verify\"", "{}", "null", "1", "[1]", "[\"verify\",2]", "[null]", "[[\"verify\"]]")
      .foreach { value =>
        assertEquals(
          intercept[JwtNonStringException](
            parseJwk(rsaJson("k", Rfc7638Modulus, ",\"key_ops\":" + value))
          ).key,
          "key_ops",
          value
        )
      }
  }

  test("parseJwk should accept a key without kid, use and alg") {
    val jwk = parseJwk("{\"kty\":\"RSA\",\"n\":\"" + Rfc7638Modulus + "\",\"e\":\"AQAB\"}")
    assertEquals((jwk.keyId, jwk.use, jwk.algorithm), (None, None, None))
    assertEquals(jwk.thumbprint.base64Url, Rfc7638Thumbprint)
  }

  test("parseJwk should ignore private members") {
    val jwk = parseJwk(rsaJson("k", Rfc7638Modulus, ",\"d\":\"SECRET-D\",\"p\":\"SECRET-P\""))
    assert(!jwk.toJson.contains("SECRET"), jwk.toJson)
    assert(!jwk.toJson.contains("\"d\""), jwk.toJson)
    val ecKey = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256)))
    val withD = ecKey.toJson.dropRight(1) + ",\"d\":\"SECRET-D\"}"
    assertEquals(parseJwk(withD), ecKey)
  }

  test("parseJwk should let the parsing failure of a document that is not JSON propagate") {
    Seq("not json", "").foreach { json => intercept[ParsingFailure](parseJwk(json)) }
  }

  test("parseJwk should reject a document that is not a JSON object") {
    Seq("[]", "\"RSA\"").foreach { json => intercept[JwtValidationException](parseJwk(json)) }
  }

  test("parseJwk should reject a key type that is not supported") {
    intercept[JwtNonSupportedAlgorithm](parseJwk("{\"kty\":\"oct\",\"k\":\"AAAA\"}"))
  }

  test("parseJwk should reject a missing or non string key parameter") {
    assertEquals(
      intercept[JwtNonStringException](parseJwk("{\"kty\":\"RSA\",\"e\":\"AQAB\"}")).key,
      "n"
    )
    assertEquals(
      intercept[JwtNonStringException](
        parseJwk("{\"kty\":\"RSA\",\"n\":12345,\"e\":\"AQAB\"}")
      ).key,
      "n"
    )
    assertEquals(
      intercept[JwtNonStringException](
        parseJwk("{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"" + b64(new Array[Byte](32)) + "\"}")
      ).key,
      "y"
    )
    assertEquals(
      intercept[JwtNonStringException](parseJwk("{\"kty\":\"EC\",\"x\":\"AA\",\"y\":\"AA\"}")).key,
      "crv"
    )
    assertEquals(
      intercept[JwtNonStringException](
        parseJwk(rsaJson("k", Rfc7638Modulus).replace("\"k\"", "7"))
      ).key,
      "kid"
    )
  }

  test("parseJwk should reject a curve that is not supported") {
    val coordinate = b64(new Array[Byte](32))
    Seq(
      "{\"kty\":\"EC\",\"crv\":\"secp256k1\",\"x\":\"" + coordinate + "\",\"y\":\"" + coordinate + "\"}",
      "{\"kty\":\"OKP\",\"crv\":\"X25519\",\"x\":\"" + coordinate + "\"}"
    ).foreach { json => intercept[JwtNonSupportedCurve](parseJwk(json)) }
  }

  test("parseJwk should reject coordinates that are not of the fixed length") {
    val key = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256))).asInstanceOf[EcJwk]
    def json(x: String, y: String) =
      "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"" + x + "\",\"y\":\"" + y + "\"}"
    val short = b64(new Array[Byte](31))
    val long = b64(new Array[Byte](33))
    Seq(
      json(short, key.y),
      json(key.x, long),
      "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + b64(new Array[Byte](31)) + "\"}",
      "{\"kty\":\"OKP\",\"crv\":\"Ed448\",\"x\":\"" + b64(new Array[Byte](32)) + "\"}"
    ).foreach { json => intercept[JwtValidationException](parseJwk(json)) }
  }

  test("parseJwk should reject a point that is not on the curve") {
    val key = ecPublic(ec(EcCurve.P384))
    Seq(
      ecJson(EcCurve.P384, key.getW.getAffineX, key.getW.getAffineY.add(BigInteger.ONE)),
      ecJson(EcCurve.P256, BigInteger.ONE, BigInteger.ONE)
    ).foreach { json => intercept[JwtValidationException](parseJwk(json)) }
  }

  test("parseJwk should let the exception of a malformed base64url parameter propagate") {
    Seq("!!!", "a+b/").foreach { n =>
      intercept[IllegalArgumentException](parseJwk(rsaJson("k", n)))
    }
  }

  test("parseJwk should reject empty parameters") {
    intercept[JwtValidationException](parseJwk(rsaJson("k", "")))
  }

  test("parseJwk should give equal OKP keys for two spellings of the same key") {
    def okpJson(x: String): String = "{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"" + x + "\"}"
    val nonCanonical = Rfc8037Ed25519X.dropRight(1) + "p"
    assertNotEquals(nonCanonical, Rfc8037Ed25519X)
    assertEquals(JwtBase64.decode(nonCanonical).toList, JwtBase64.decode(Rfc8037Ed25519X).toList)
    val parsed = parseJwk(okpJson(nonCanonical))
    assertEquals(parsed, parseJwk(okpJson(Rfc8037Ed25519X)))
    assertEquals(parsed.asInstanceOf[OkpJwk].x, Rfc8037Ed25519X)
    assertEquals(parsed.thumbprint.base64Url, Rfc8037Ed25519Thumbprint)
  }

  test("parseJwk should leave an RSA key the JDK refuses to fail when its public key is built") {
    val jwk = parseJwk(rsaJson("k", "AQAB"))
    intercept[InvalidKeySpecException](jwk.publicKey)
  }

  private val jwksFixture: String = {
    val second = Jwk.fromPublicKey(rsaPublic(rsa())).asInstanceOf[RsaJwk]
    val ec256 = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256)), Some("ec-1"), Some(JwkUse.Signature))
    val coordinate = b64(new Array[Byte](32))
    val short = b64(new Array[Byte](31))
    val offCurve = ecJson(EcCurve.P256, BigInteger.ONE, BigInteger.ONE)
    "{\"keys\":[" + Seq(
      rsaJson("rsa-1", Rfc7638Modulus),
      rsaJson(
        "rsa-2",
        second.n,
        ",\"d\":\"SECRET-D\",\"p\":\"SECRET-P\",\"q\":\"SECRET-Q\",\"dp\":\"SECRET\",\"dq\":\"SECRET\",\"qi\":\"SECRET\""
      ),
      ec256.toJson,
      "{\"kty\":\"oct\",\"kid\":\"hmac\",\"k\":\"c2VjcmV0\"}",
      "{\"kty\":\"OKP\",\"crv\":\"X25519\",\"kid\":\"x25519\",\"x\":\"" + coordinate + "\"}",
      "{\"kty\":\"EC\",\"crv\":\"secp256k1\",\"kid\":\"k1\",\"x\":\"" + coordinate + "\",\"y\":\"" + coordinate + "\"}",
      "{\"kty\":\"EC\",\"crv\":\"P-256\",\"kid\":\"short\",\"x\":\"" + short + "\",\"y\":\"" + short + "\"}",
      offCurve.replace("{", "{\"kid\":\"off-curve\","),
      rsaJson("bad-n", "!!!"),
      "{\"kid\":\"no-kty\"}",
      "\"a string\"",
      "42",
      "null",
      "{\"kty\":\"RSA\",\"kid\":\"no-n\",\"e\":\"AQAB\"}"
    ).mkString(",") + "]}"
  }

  test("parseJwkSet should keep the usable keys and skip the others") {
    val set = JwtCirce.parseJwkSet(jwksFixture)
    assertEquals(set.keys.map(_.keyId), Seq(Some("rsa-1"), Some("rsa-2"), Some("ec-1")))
    assertEquals(set.keys.map(_.getClass.getSimpleName), Seq("RsaJwk", "RsaJwk", "EcJwk"))
    assertEquals(set.find("rsa-1").map(_.algorithm), Some(Some("RS256")))
    assertEquals(set.find("rsa-1").map(_.use), Some(Some(JwkUse.Signature)))
    assertEquals(set.find("rsa-1").map(_.thumbprint.base64Url), Some(Rfc7638Thumbprint))
    assertEquals(set.find("hmac"), None)
    assertEquals(set.find("x25519"), None)
  }

  test("parseJwkSet should skip keys that fail with any exception and keep the good one") {
    val good = Jwk.fromPublicKey(rsaPublic(rsa()), Some("good"))
    val json = "{\"keys\":[" + Seq(
      rsaJson("bad-base64", "!!!"),
      rsaJson("bad-empty", ""),
      "{\"kty\":\"RSA\",\"kid\":\"no-n\",\"e\":\"AQAB\"}",
      good.toJson
    ).mkString(",") + "]}"
    assertEquals(JwtCirce.parseJwkSet(json), JwkSet(Seq(good)))
  }

  test("parseJwkSet should skip the keys whose key_ops is malformed") {
    val json = "{\"keys\":[" + Seq(
      rsaJson("ok", Rfc7638Modulus, ",\"key_ops\":[\"verify\"]"),
      rsaJson("bad-string", Rfc7638Modulus, ",\"key_ops\":\"verify\""),
      rsaJson("bad-item", Rfc7638Modulus, ",\"key_ops\":[\"verify\",1]"),
      rsaJson("none", Rfc7638Modulus)
    ).mkString(",") + "]}"
    val set = JwtCirce.parseJwkSet(json)
    assertEquals(set.keys.map(_.keyId), Seq(Some("ok"), Some("none")))
    assertEquals(set.keys.map(_.keyOperations), Seq(Some(Set("verify")), None))
  }

  test("parseJwkSet should never read or write private members") {
    val json = JwtCirce.parseJwkSet(jwksFixture).toJson
    assert(!json.contains("SECRET"), json)
    assert(!json.contains("\"d\""), json)
    assert(!json.contains("\"p\""), json)
  }

  test("parseJwkSet should round trip through toJson") {
    val set = JwtCirce.parseJwkSet(jwksFixture)
    assertEquals(JwtCirce.parseJwkSet(set.toJson), set)
    val all = JwkSet(
      Seq(
        Jwk.fromPublicKey(rsaPublic(rsa()), Some("a")),
        Jwk.fromPublicKey(ecPublic(ec(EcCurve.P521)), Some("b")),
        Jwk.fromPublicKey(okp(OkpCurve.Ed25519).getPublic, Some("c")),
        Jwk.fromPublicKey(okp(OkpCurve.Ed448).getPublic, Some("d"))
      )
    )
    assertEquals(JwtCirce.parseJwkSet(all.toJson), all)
  }

  test("parseJwkSet should round trip a kid with a backslash, a quote and a control character") {
    val kid = "a\\b\"c\u0001d\ne\tf/g"
    val jwk = Jwk.fromPublicKey(rsaPublic(rsa()), Some(kid), Some(JwkUse.Signature))
    val set = JwtCirce.parseJwkSet(JwkSet(Seq(jwk)).toJson)
    assertEquals(set.keys.map(_.keyId), Seq(Some(kid)))
    assertEquals(set, JwkSet(Seq(jwk)))
    assertEquals(JwtCirce.parseJwk(jwk.toJson).keyId, Some(kid))
  }

  test("parseJwkSet should accept an empty set") {
    assertEquals(JwtCirce.parseJwkSet("{\"keys\":[]}"), JwkSet(Seq.empty))
  }

  test("parseJwkSet should let the parsing failure of a document that is not JSON propagate") {
    intercept[ParsingFailure](JwtCirce.parseJwkSet("not json"))
  }
}
