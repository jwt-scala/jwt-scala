package pdi.jwt

import java.math.BigInteger
import java.security.spec.InvalidKeySpecException
import java.security.{KeyPair, PrivateKey, SignatureException}
import java.time.Instant
import scala.util.{Failure, Success, Try}

import io.circe.ParsingFailure
import pdi.jwt.JwtAlgorithm.*
import pdi.jwt.algorithms.JwtAsymmetricAlgorithm
import pdi.jwt.exceptions.*

class JwtCirceDecodeWithJwksSpec extends munit.FunSuite {
  import JwkTestKeys.*

  private case class Signer(kid: String, algorithm: JwtAsymmetricAlgorithm, pair: KeyPair)

  private val signers = Seq(
    Signer("rsa-256", RS256, rsa()),
    Signer("rsa-384", RS384, rsa()),
    Signer("rsa-512", RS512, rsa()),
    Signer("p256", ES256, ec(EcCurve.P256)),
    Signer("p384", ES384, ec(EcCurve.P384)),
    Signer("p521", ES512, ec(EcCurve.P521)),
    Signer("ed25519", EdDSA, okp(OkpCurve.Ed25519)),
    Signer("ed448", EdDSA, okp(OkpCurve.Ed448))
  )

  private def signer(kid: String): Signer = signers.find(_.kid == kid).get

  private val keys = JwkSet(signers.map { s =>
    Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid), Some(JwkUse.Signature), Some(s.algorithm.name))
  })

  private val allowed: Seq[JwtAsymmetricAlgorithm] = JwtAlgorithm.allAsymmetric()

  private def inOneHour = Instant.now.getEpochSecond + 3600

  private def claim(expiration: Long = inOneHour) =
    JwtClaim(subject = Some("user-1"), expiration = Some(expiration))

  private def token(
      kid: Option[String],
      algorithm: JwtAsymmetricAlgorithm,
      key: PrivateKey,
      body: JwtClaim = claim()
  ): String =
    JwtCirce.encode(
      JwtHeader(Some(algorithm), Some("JWT"), None, kid).toJson,
      body.toJson,
      key,
      algorithm
    )

  private def tokenFor(s: Signer): String =
    token(Some(s.kid), s.algorithm, s.pair.getPrivate)

  private def decode(
      token: String,
      jwks: JwkSet = keys,
      algorithms: Seq[JwtAsymmetricAlgorithm] = allowed,
      options: JwtOptions = JwtOptions.DEFAULT
  ): Try[JwtClaim] = JwtCirce.decodeWithJwks(token, jwks, algorithms, options)

  private def tamper(token: String): String = {
    val parts = token.split('.')
    val signature = JwtBase64.decode(parts(2))
    signature(signature.length - 1) = (signature(signature.length - 1) ^ 1).toByte
    parts(0) + "." + parts(1) + "." + JwtBase64.encodeString(signature)
  }

  signers.foreach { s =>
    test(s"decodeWithJwks should verify a ${s.algorithm.name} token with the key ${s.kid}") {
      val result = decode(tokenFor(s))
      assertEquals(result.map(_.subject), Success(Some("user-1")))
    }
  }

  test("decodeWithJwks should select the key by kid among several of the same type") {
    val other = signer("rsa-384")
    val wrongKey = token(Some("rsa-256"), RS256, other.pair.getPrivate)
    assert(decode(token(Some("rsa-256"), RS256, signer("rsa-256").pair.getPrivate)).isSuccess)
    intercept[JwtValidationException](decode(wrongKey).get)
  }

  test("decodeWithJwks should fail with an unknown kid") {
    val s = signer("rsa-256")
    val result = decode(token(Some("unknown"), s.algorithm, s.pair.getPrivate))
    assertEquals(intercept[JwtUnknownKeyIdException](result.get).keyId, "unknown")
    assertEquals(result.failed.get.getMessage, "No key of the set has the key id of the token.")
    result match {
      case Failure(JwtUnknownKeyIdException(keyId)) => assertEquals(keyId, "unknown")
      case other                                    => fail(s"Unexpected $other")
    }
  }

  test("decodeWithJwks should keep a JwtValidationException for a kid whose key is unusable") {
    val s = signer("rsa-256")
    val result = decode(token(Some("p256"), RS256, s.pair.getPrivate))
    assert(!result.failed.get.isInstanceOf[JwtUnknownKeyIdException])
    intercept[JwtValidationException](result.get)
  }

  test("decodeWithJwks should fail without a kid") {
    val s = signer("rsa-256")
    val result = decode(token(None, s.algorithm, s.pair.getPrivate))
    assert(!result.failed.get.isInstanceOf[JwtUnknownKeyIdException])
    intercept[JwtValidationException](result.get)
  }

  test("decodeWithJwks should not match keys that have no kid") {
    val s = signer("rsa-256")
    val noKid = JwkSet(Seq(Jwk.fromPublicKey(s.pair.getPublic)))
    intercept[JwtUnknownKeyIdException](decode(tokenFor(s), noKid).get)
  }

  test("decodeWithJwks should fail when the algorithm is not allowed") {
    val s = signer("rsa-256")
    val result = decode(tokenFor(s), algorithms = Seq(ES256, RS384))
    assertEquals(
      intercept[JwtValidationException](result.get).getMessage,
      "Invalid signature for this token or wrong algorithm."
    )
  }

  test("decodeWithJwks should fail when the token uses an HMAC algorithm") {
    val token = JwtCirce.encode(
      JwtHeader(Some(HS256), Some("JWT"), None, Some("rsa-256")).toJson,
      claim().toJson,
      "secret",
      HS256
    )
    intercept[JwtValidationException](decode(token).get)
  }

  test("decodeWithJwks should fail for an alg none token") {
    val unsigned = JwtBase64.encodeString("{\"alg\":\"none\",\"kid\":\"rsa-256\"}") + "." +
      JwtBase64.encodeString(claim().toJson) + "."
    intercept[JwtEmptyAlgorithmException](decode(unsigned).get)
  }

  test("decodeWithJwks should fail for an alg none token that has a signature") {
    val withSignature = tamper(tokenFor(signer("rsa-256")))
    val noneWithSignature = JwtBase64.encodeString("{\"alg\":\"none\",\"kid\":\"rsa-256\"}") +
      "." + withSignature.split('.').drop(1).mkString(".")
    intercept[JwtEmptyAlgorithmException](decode(noneWithSignature).get)
  }

  test("decodeWithJwks should fail when an RS256 token names an EC key") {
    intercept[JwtValidationException](
      decode(token(Some("p256"), RS256, signer("rsa-256").pair.getPrivate)).get
    )
  }

  test("decodeWithJwks should fail when an ES256 token names an RSA key") {
    intercept[JwtValidationException](
      decode(token(Some("rsa-256"), ES256, signer("p256").pair.getPrivate)).get
    )
  }

  test("decodeWithJwks should fail when the token uses ES256 with a key of another curve") {
    Seq(
      token(Some("p384"), ES256, signer("p256").pair.getPrivate),
      token(Some("p256"), ES384, signer("p384").pair.getPrivate),
      token(Some("p521"), ES256, signer("p256").pair.getPrivate)
    ).foreach { t => intercept[JwtValidationException](decode(t).get) }
  }

  test("decodeWithJwks should fail when EdDSA names a key that is not OKP") {
    intercept[JwtValidationException](
      decode(token(Some("p256"), EdDSA, signer("ed25519").pair.getPrivate)).get
    )
  }

  test("decodeWithJwks should fail when the alg of the key differs from the header") {
    val s = signer("rsa-256")
    val jwk =
      Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid), Some(JwkUse.Signature), Some("RS384"))
    intercept[JwtValidationException](decode(tokenFor(s), JwkSet(Seq(jwk))).get)
  }

  test("decodeWithJwks should accept a key without alg and without use") {
    val s = signer("p256")
    val jwk = Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid))
    assert(decode(tokenFor(s), JwkSet(Seq(jwk))).isSuccess)
  }

  test("decodeWithJwks should use the key of the type that fits when a kid is duplicated") {
    val ecSigner = signer("p256")
    val rsaSigner = signer("rsa-256")
    val ecJwk = Jwk.fromPublicKey(ecSigner.pair.getPublic, Some("dup"))
    val rsaJwk = Jwk.fromPublicKey(rsaSigner.pair.getPublic, Some("dup"))
    val rsaToken = token(Some("dup"), RS256, rsaSigner.pair.getPrivate)
    val ecToken = token(Some("dup"), ES256, ecSigner.pair.getPrivate)
    Seq(JwkSet(Seq(ecJwk, rsaJwk)), JwkSet(Seq(rsaJwk, ecJwk))).foreach { set =>
      assertEquals(decode(rsaToken, set).map(_.subject), Success(Some("user-1")))
      assertEquals(decode(ecToken, set).map(_.subject), Success(Some("user-1")))
    }
  }

  test("decodeWithJwks should use the key meant for signatures when a kid is duplicated") {
    val encrypting = rsa()
    val signing = signer("rsa-256")
    val set = JwkSet(
      Seq(
        Jwk.fromPublicKey(encrypting.getPublic, Some("dup"), Some(JwkUse.Encryption)),
        Jwk.fromPublicKey(signing.pair.getPublic, Some("dup"), Some(JwkUse.Signature))
      )
    )
    val signed = token(Some("dup"), RS256, signing.pair.getPrivate)
    assertEquals(decode(signed, set).map(_.subject), Success(Some("user-1")))
    val encryptingOnly = JwkSet(set.keys.take(1))
    intercept[JwtValidationException](decode(signed, encryptingOnly).get)
  }

  test(
    "decodeWithJwks should use the key whose alg is the one of the token when a kid is duplicated"
  ) {
    val other = rsa()
    val signing = signer("rsa-256")
    val set = JwkSet(
      Seq(
        Jwk.fromPublicKey(other.getPublic, Some("dup"), None, Some("RS384")),
        Jwk.fromPublicKey(signing.pair.getPublic, Some("dup"), None, Some("RS256"))
      )
    )
    val signed = token(Some("dup"), RS256, signing.pair.getPrivate)
    assertEquals(decode(signed, set).map(_.subject), Success(Some("user-1")))
  }

  test("decodeWithJwks should fail when no key sharing the kid is usable") {
    val rsaSigner = signer("rsa-256")
    val signed = token(Some("dup"), RS256, rsaSigner.pair.getPrivate)
    val ecSet = JwkSet(
      Seq(
        Jwk.fromPublicKey(signer("p256").pair.getPublic, Some("dup")),
        Jwk.fromPublicKey(signer("p384").pair.getPublic, Some("dup"))
      )
    )
    val result = decode(signed, ecSet)
    assertEquals(
      intercept[JwtValidationException](result.get).getMessage,
      "The type or curve of the key doesn't match the algorithm."
    )
    assert(!result.failed.get.isInstanceOf[JwtUnknownKeyIdException])
  }

  test("decodeWithJwks should report the failure of the first key sharing the kid") {
    val rsaSigner = signer("rsa-256")
    val signed = token(Some("dup"), RS256, rsaSigner.pair.getPrivate)
    val encrypting =
      Jwk.fromPublicKey(rsaSigner.pair.getPublic, Some("dup"), Some(JwkUse.Encryption))
    val ec = Jwk.fromPublicKey(signer("p256").pair.getPublic, Some("dup"))
    assertEquals(
      intercept[JwtValidationException](decode(signed, JwkSet(Seq(encrypting, ec))).get).getMessage,
      "The key is meant for encryption, not for signatures."
    )
    assertEquals(
      intercept[JwtValidationException](decode(signed, JwkSet(Seq(ec, encrypting))).get).getMessage,
      "The type or curve of the key doesn't match the algorithm."
    )
  }

  test("decodeWithJwks should still verify the signature with the selected key only") {
    val signed = token(Some("dup"), RS256, signer("rsa-256").pair.getPrivate)
    val set = JwkSet(
      Seq(
        Jwk.fromPublicKey(signer("rsa-384").pair.getPublic, Some("dup")),
        Jwk.fromPublicKey(signer("rsa-256").pair.getPublic, Some("dup"))
      )
    )
    intercept[JwtValidationException](decode(signed, set).get)
  }

  private def withOps(s: Signer, ops: Option[Set[String]]): JwkSet =
    JwkSet(Seq(Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid), keyOperations = ops)))

  test("decodeWithJwks should accept a key whose key_ops contains verify") {
    signers.foreach { s =>
      assert(decode(tokenFor(s), withOps(s, Some(Set("verify")))).isSuccess, s.kid)
      assert(decode(tokenFor(s), withOps(s, Some(Set("sign", "verify")))).isSuccess, s.kid)
    }
  }

  test("decodeWithJwks should accept a key without key_ops") {
    val s = signer("rsa-256")
    assert(decode(tokenFor(s), withOps(s, None)).isSuccess)
  }

  test("decodeWithJwks should fail when key_ops does not contain verify") {
    val s = signer("rsa-256")
    Seq(Set("sign"), Set("encrypt", "wrapKey"), Set.empty[String]).foreach { ops =>
      val result = decode(tokenFor(s), withOps(s, Some(ops)))
      assertEquals(
        intercept[JwtValidationException](result.get).getMessage,
        "The key is not meant to verify signatures (key_ops)."
      )
    }
  }

  test("decodeWithJwks should use the key whose key_ops allows verify when a kid is duplicated") {
    val s = signer("rsa-256")
    val signOnly =
      Jwk.fromPublicKey(rsa().getPublic, Some("dup"), keyOperations = Some(Set("sign")))
    val verifying =
      Jwk.fromPublicKey(s.pair.getPublic, Some("dup"), keyOperations = Some(Set("verify")))
    val signed = token(Some("dup"), RS256, s.pair.getPrivate)
    assert(decode(signed, JwkSet(Seq(signOnly, verifying))).isSuccess)
    intercept[JwtValidationException](decode(signed, JwkSet(Seq(signOnly))).get)
  }

  private def withOpsDocument(s: Signer, ops: String): JwkSet = {
    val jwk = Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid))
    JwtCirce.parseJwkSet("{\"keys\":[" + jwk.toJson.dropRight(1) + ",\"key_ops\":" + ops + "}]}")
  }

  test("decodeWithJwks should parse key_ops from a JWK Set document") {
    val s = signer("rsa-256")
    assert(decode(tokenFor(s), withOpsDocument(s, "[\"verify\"]")).isSuccess)
    intercept[JwtValidationException](decode(tokenFor(s), withOpsDocument(s, "[\"sign\"]")).get)
  }

  test("decodeWithJwks should not find a key whose key_ops is malformed in a JWK Set document") {
    val s = signer("rsa-256")
    intercept[JwtUnknownKeyIdException](decode(tokenFor(s), withOpsDocument(s, "\"verify\"")).get)
  }

  test("decodeWithJwks should fail when the key is meant for encryption") {
    val s = signer("rsa-256")
    val jwk = Jwk.fromPublicKey(s.pair.getPublic, Some(s.kid), Some(JwkUse.Encryption))
    intercept[JwtValidationException](decode(tokenFor(s), JwkSet(Seq(jwk))).get)
  }

  test("decodeWithJwks should fail with a tampered signature") {
    signers.filter(_.algorithm != EdDSA).foreach { s =>
      intercept[JwtValidationException](decode(tamper(tokenFor(s))).get)
    }
  }

  test("decodeWithJwks should let the SignatureException of decode through") {
    val parts = tokenFor(signer("rsa-256")).split('.')
    val truncated = JwtBase64.encodeString(JwtBase64.decode(parts(2)).dropRight(1))
    intercept[SignatureException](decode(parts(0) + "." + parts(1) + "." + truncated).get)
  }

  test("decodeWithJwks should fail with a tampered claim") {
    val s = signer("rsa-256")
    val parts = tokenFor(s).split('.')
    val forged =
      JwtBase64.encodeString(JwtClaim(subject = Some("admin"), expiration = Some(inOneHour)).toJson)
    intercept[JwtValidationException](decode(parts(0) + "." + forged + "." + parts(2)).get)
  }

  test("decodeWithJwks should fail with a token signed by another key") {
    val s = signer("p256")
    intercept[JwtValidationException](
      decode(token(Some(s.kid), ES256, ec(EcCurve.P256).getPrivate)).get
    )
  }

  test("decodeWithJwks should honour the options") {
    val s = signer("rsa-256")
    val expired =
      token(Some(s.kid), s.algorithm, s.pair.getPrivate, claim(Instant.now.getEpochSecond - 3600))
    assert(decode(expired, options = JwtOptions(expiration = false)).isSuccess)
    assert(decode(expired, options = JwtOptions(leeway = 7200)).isSuccess)
    intercept[JwtExpirationException](decode(expired).get)
  }

  test("decodeWithJwks should use the default options when none are given") {
    val s = signer("rsa-256")
    val expired =
      token(Some(s.kid), s.algorithm, s.pair.getPrivate, claim(Instant.now.getEpochSecond - 3600))
    intercept[JwtExpirationException](JwtCirce.decodeWithJwks(expired, keys, allowed).get)
  }

  test("decodeWithJwks should return a Failure for a token that does not have three parts") {
    Seq("abc", "a.b.c.d").foreach { t => intercept[JwtLengthException](decode(t).get) }
  }

  test("decodeWithJwks should return a Failure for a token without alg") {
    intercept[JwtEmptyAlgorithmException](decode(JwtBase64.encodeString("{}") + ".e30.AA").get)
  }

  test(
    "decodeWithJwks should fail with the exception of decode for a header that is not base64url"
  ) {
    intercept[IllegalArgumentException](decode("!!!.e30.AA").get)
  }

  test("decodeWithJwks should fail with the exception of decode for a header that is not JSON") {
    intercept[ParsingFailure](decode(JwtBase64.encodeString("not json") + ".e30.AA").get)
  }

  test("decodeWithJwks should fail with the exception of decode for a malformed signature") {
    val s = signer("rsa-256")
    val parts = tokenFor(s).split('.')
    intercept[IllegalArgumentException](decode(parts(0) + "." + parts(1) + ".!!!").get)
  }

  test("decodeWithJwks should fail with an empty set") {
    val s = signer("rsa-256")
    intercept[JwtUnknownKeyIdException](decode(tokenFor(s), JwkSet(Seq.empty)).get)
  }

  test("decodeWithJwks should fail with an empty list of algorithms") {
    val s = signer("rsa-256")
    intercept[JwtValidationException](decode(tokenFor(s), algorithms = Seq.empty).get)
  }

  test("decodeWithJwks should fail with the exception of the JDK for a key it refuses") {
    val s = signer("rsa-256")
    val refused =
      RsaJwk(Some(s.kid), None, None, BigInteger.valueOf(65537), BigInteger.valueOf(65537))
    intercept[InvalidKeySpecException](decode(tokenFor(s), JwkSet(Seq(refused))).get)
  }

  test("decodeWithJwks should never throw") {
    val result = decode("%%%")
    assert(result.isInstanceOf[Failure[?]])
  }

  test("decodeWithJwks should not echo the token or the keys in its messages") {
    val s = signer("rsa-256")
    val messages = Seq(
      decode(token(Some("unknown"), s.algorithm, s.pair.getPrivate)),
      decode(token(None, s.algorithm, s.pair.getPrivate)),
      decode(tamper(tokenFor(s))),
      decode(token(Some("p256"), RS256, s.pair.getPrivate))
    ).map(_.failed.get.getMessage)
    messages.foreach { message =>
      assert(!message.contains("unknown"), message)
      assert(!message.contains(b64(rsaPublic(s.pair).getModulus.toByteArray)), message)
    }
  }

  private def b64(bytes: Array[Byte]): String = JwtBase64.encodeString(bytes)
}
