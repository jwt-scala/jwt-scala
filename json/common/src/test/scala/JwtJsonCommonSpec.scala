package pdi.jwt

import java.time.Clock
import scala.util.{Failure, Success}

import pdi.jwt.exceptions.{
  JwtNonStringException,
  JwtNonSupportedAlgorithm,
  JwtNonSupportedCurve,
  JwtUnknownKeyIdException,
  JwtValidationException
}

abstract class JwtJsonCommonSpec[J] extends munit.FunSuite with JsonCommonFixture[J] {
  import JwtJsonCommonSpec.*

  protected def jwtJsonCommon(clock: Clock): JwtJsonUnderTest[J]

  protected def testEncoding: Boolean = true

  lazy val defaultJwt: JwtJsonUnderTest[J] = jwtJsonCommon(Clock.systemUTC)
  lazy val afterExpirationJwt: JwtJsonUnderTest[J] = jwtJsonCommon(afterExpirationClock)
  lazy val validTimeJwt: JwtJsonUnderTest[J] = jwtJsonCommon(validTimeClock)

  def battleTestEncode(d: JsonDataEntryTrait[J], key: String, jwt: JwtJsonUnderTest[J]) = {
    assertEquals(d.token, jwt.encode(d.headerJson, claimJson, key), s"${d.algo.fullName} key")
    assertEquals(
      d.tokenEmpty,
      jwt.encode(claimJson),
      s"${d.algo.fullName} No header, no key, no algo"
    )
    assertEquals(
      d.token,
      jwt.encode(claimJson, key, d.algo),
      s"${d.algo.fullName} No header, key, algo"
    )
  }

  if (testEncoding) {
    test("should encode with no algorithm") {
      assertEquals(tokenEmpty, { defaultJwt.encode(headerEmptyJson, claimJson) }, "Unsigned key")
    }

    test("should encode HMAC") {
      dataJson.foreach { d => battleTestEncode(d, secretKey, defaultJwt) }
    }

    test("should encode RSA") {
      dataRSAJson.foreach { d => battleTestEncode(d, privateKeyRSA, defaultJwt) }
    }
  }

  test("should decodeJsonAll") {
    dataJson.foreach { d =>
      val success = Success((d.headerJson, claimJson, d.signature))
      assertEquals(
        validTimeJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac()),
        success,
        d.algo.fullName
      )
      assertEquals(
        validTimeJwt.decodeJsonAll(d.token, secretKeyOf(d.algo)),
        success,
        d.algo.fullName
      )
    }

    dataRSAJson.foreach { d =>
      val success = Success((d.headerJson, claimJson, d.signature))
      assertEquals(
        validTimeJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        success,
        d.algo.fullName
      )
    }
  }

  test("should decodeJson") {
    val success = Success(claimJson)

    dataJson.foreach { d =>
      assertEquals(
        validTimeJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac()),
        success,
        d.algo.fullName
      )
      assertEquals(validTimeJwt.decodeJson(d.token, secretKeyOf(d.algo)), success, d.algo.fullName)
    }

    dataRSAJson.foreach { d =>
      assertEquals(
        validTimeJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        success,
        d.algo.fullName
      )
    }
  }

  test("should decodeWithJwks with the key of the set that has the kid of the token") {
    val keys = JwkSet(
      Seq(
        Jwk.fromPublicKey(JwtUtils.parsePublicKey(publicKeyRSA, JwtUtils.RSA), Some("rsa-1"))
      )
    )
    def tokenWith(kid: String): String = validTimeJwt.encode(
      s"""{"typ":"JWT","alg":"RS256","kid":"$kid"}""",
      claim,
      privateKeyRSA,
      JwtAlgorithm.RS256
    )

    val algorithms = JwtAlgorithm.allRSA()

    assertEquals(
      validTimeJwt.decodeWithJwks(tokenWith("rsa-1"), keys, algorithms),
      Success(claimClass)
    )
    validTimeJwt.decodeWithJwks(tokenWith("other"), keys, algorithms) match {
      case Failure(e: JwtUnknownKeyIdException) => assertEquals(e.keyId, "other")
      case other                                => fail(s"Unexpected result: $other")
    }
  }

  test("should decodeAll") {
    dataJson.foreach { d =>
      val success = Success((d.headerClass, claimClass, d.signature))
      assertEquals(
        validTimeJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac()),
        success,
        d.algo.fullName
      )
      assertEquals(validTimeJwt.decodeAll(d.token, secretKeyOf(d.algo)), success, d.algo.fullName)
    }

    dataRSAJson.foreach { d =>
      val success = Success((d.headerClass, claimClass, d.signature))
      assertEquals(
        validTimeJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        success,
        d.algo.fullName
      )
    }
  }

  test("should fail to decodeJsonAll and decodeJson when now is after expiration date") {
    dataJson.foreach { d =>
      assert(afterExpirationJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure)

      assert(afterExpirationJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure)

      assert(afterExpirationJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure)
    }

    dataRSAJson.foreach { d =>
      assert(
        afterExpirationJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure
      )

      assert(afterExpirationJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure)

      assert(afterExpirationJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure)
    }
  }

  test(
    "should success to decodeJsonAll and decodeJson when now is after expiration date with options"
  ) {
    val options = JwtOptions(expiration = false)

    dataJson.foreach { d =>
      assert(
        afterExpirationJwt
          .decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )

      assert(
        afterExpirationJwt
          .decodeJson(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )

      assert(
        afterExpirationJwt
          .decodeAll(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )
    }

    dataRSAJson.foreach { d =>
      assert(
        afterExpirationJwt
          .decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )

      assert(
        afterExpirationJwt
          .decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )

      assert(
        afterExpirationJwt
          .decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )
    }
  }

  test("should parseJwk an RSA key and ignore its private members") {
    val jwk = defaultJwt.parseJwk(
      s"""{"kty":"RSA","kid":"rsa-1","use":"sig","alg":"RS256","key_ops":["verify"],"n":"$RsaModulus","e":"AQAB","d":"SECRET-D","p":"SECRET-P"}"""
    )
    jwk match {
      case rsa: RsaJwk =>
        assertEquals(rsa.keyId, Some("rsa-1"))
        assertEquals(rsa.use, Some(JwkUse.Signature))
        assertEquals(rsa.algorithm, Some("RS256"))
        assertEquals(rsa.keyOperations, Some(Set("verify")))
        assertEquals(rsa.n, RsaModulus)
        assertEquals(rsa.e, "AQAB")
      case other => fail(s"Unexpected key: $other")
    }
    assertEquals(jwk.thumbprint.base64Url, RsaThumbprint)
    assert(!jwk.toJson.contains("SECRET"), jwk.toJson)
  }

  test("should parseJwk an EC key") {
    val jwk = defaultJwt.parseJwk(
      s"""{"kty":"EC","crv":"P-256","kid":"ec-1","x":"$EcX","y":"$EcY"}"""
    )
    jwk match {
      case ec: EcJwk =>
        assertEquals(ec.keyId, Some("ec-1"))
        assertEquals(ec.curve, EcCurve.P256)
        assertEquals(ec.x, EcX)
        assertEquals(ec.y, EcY)
      case other => fail(s"Unexpected key: $other")
    }
  }

  test("should parseJwk an OKP key") {
    val jwk = defaultJwt.parseJwk(s"""{"kty":"OKP","crv":"Ed25519","x":"$Ed25519X"}""")
    jwk match {
      case okp: OkpJwk =>
        assertEquals(okp.curve, OkpCurve.Ed25519)
        assertEquals(okp.x, Ed25519X)
      case other => fail(s"Unexpected key: $other")
    }
    assertEquals(jwk.thumbprint.base64Url, Ed25519Thumbprint)
  }

  test("should parseJwk a key that was written by toJson") {
    val jwk = Jwk.fromPublicKey(
      JwtUtils.parsePublicKey(publicKeyRSA, JwtUtils.RSA),
      Some("rsa-1"),
      Some(JwkUse.Signature),
      Some("RS256"),
      Some(Set("verify"))
    )
    assertEquals(defaultJwt.parseJwk(jwk.toJson), jwk)
  }

  test("should not parseJwk a document that is not a JSON object") {
    Seq("[]", """[{"kty":"RSA"}]""").foreach { json =>
      intercept[JwtValidationException](defaultJwt.parseJwk(json))
    }
  }

  test("should not parseJwk a missing or non string member") {
    def keyOf(json: String): String =
      intercept[JwtNonStringException](defaultJwt.parseJwk(json)).key
    def rsa(extra: String): String = s"""{"kty":"RSA","n":"$RsaModulus","e":"AQAB"$extra}"""

    assertEquals(keyOf("{}"), "kty")
    assertEquals(keyOf("""{"kty":1}"""), "kty")
    assertEquals(keyOf("""{"kty":"RSA","e":"AQAB"}"""), "n")
    assertEquals(keyOf(rsa(""","kid":7""")), "kid")
    assertEquals(keyOf(rsa(""","kid":null""")), "kid")
    assertEquals(keyOf(rsa(""","kid":["a"]""")), "kid")
    Seq("\"verify\"", "{}", "null", "1", "[1]", "[\"verify\",2]", "[null]", "[[\"verify\"]]")
      .foreach { value => assertEquals(keyOf(rsa(s""","key_ops":$value""")), "key_ops", value) }
  }

  test("should not parseJwk a key type that is not supported") {
    intercept[JwtNonSupportedAlgorithm](defaultJwt.parseJwk("""{"kty":"oct","k":"AAAA"}"""))
  }

  test("should not parseJwk a curve that is not supported") {
    intercept[JwtNonSupportedCurve](
      defaultJwt.parseJwk(s"""{"kty":"OKP","crv":"X25519","x":"$Ed25519X"}""")
    )
  }

  test("should let the parse error of the JSON library through when parsing a JWK") {
    intercept[Exception](defaultJwt.parseJwk("not json"))
  }

  test("should let the parse error of the JSON library through when parsing a JWK Set") {
    intercept[Exception](defaultJwt.parseJwkSet("not json"))
  }

  test("should parseJwkSet and skip the keys that are not usable") {
    val set = defaultJwt.parseJwkSet(s"""{"keys":[
      {"kty":"RSA","kid":"rsa-1","n":"$RsaModulus","e":"AQAB"},
      {"kty":"oct","kid":"hmac","k":"c2VjcmV0"},
      "a string",
      42,
      null,
      [],
      {"kid":"no-kty"},
      {"kty":"RSA","kid":"no-n","e":"AQAB"},
      {"kty":"RSA","kid":"bad-key-ops","n":"$RsaModulus","e":"AQAB","key_ops":"verify"},
      {"kty":"EC","crv":"P-256","kid":"ec-1","x":"$EcX","y":"$EcY"},
      {"kty":"OKP","crv":"Ed25519","kid":"okp-1","x":"$Ed25519X"}
    ]}""")
    assertEquals(set.keys.map(_.keyId), Seq(Some("rsa-1"), Some("ec-1"), Some("okp-1")))
    assertEquals(set.keys.map(_.keyType), Seq("RSA", "EC", "OKP"))
    assertEquals(set.find("rsa-1").map(_.thumbprint.base64Url), Some(RsaThumbprint))
    assertEquals(set.find("okp-1").map(_.thumbprint.base64Url), Some(Ed25519Thumbprint))
    assertEquals(set.find("hmac"), None)
  }

  test("should parseJwkSet with no usable keys") {
    Seq(
      """{"keys":[]}""",
      """{"keys":[null,42,"a string",[],{}, {"kty":"oct","k":"c2VjcmV0"}]}"""
    ).foreach { json => assertEquals(defaultJwt.parseJwkSet(json), JwkSet(Seq.empty)) }
  }

  test("should read JWK set candidates once and preserve usable key order") {
    var attempted = Vector.empty[Int]
    val set = JwtJsonCommon.readJwkSetKeys(Iterator(0, 1, 2)) { index =>
      attempted :+= index
      if (index == 1) throw new IllegalArgumentException("Unusable key")
      defaultJwt.parseJwk(s"""{"kty":"RSA","kid":"$index","n":"$RsaModulus","e":"AQAB"}""")
    }
    assertEquals(attempted, Vector(0, 1, 2))
    assertEquals(set.keys.map(_.keyId), List(Some("0"), Some("2")))
  }

  test("should parseJwkSet that round trips through toJson") {
    val set = defaultJwt.parseJwkSet(
      s"""{"keys":[{"kty":"RSA","kid":"rsa-1","use":"sig","n":"$RsaModulus","e":"AQAB"},{"kty":"OKP","crv":"Ed25519","kid":"okp-1","x":"$Ed25519X"}]}"""
    )
    assertEquals(set.keys.size, 2)
    assertEquals(defaultJwt.parseJwkSet(set.toJson), set)
  }

  test("should not parseJwkSet a document that has no keys array") {
    Seq("{}", """{"keys":{}}""", """{"keys":"x"}""", """{"keys":null}""", "[]")
      .foreach { json => intercept[JwtValidationException](defaultJwt.parseJwkSet(json)) }
  }
}

object JwtJsonCommonSpec {
  type JwtJsonUnderTest[J] = JwtJsonCommon[J, JwtHeader, JwtClaim]

  val RsaModulus =
    "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPeb" +
      "WKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgn" +
      "Yb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44" +
      "-csFCur-kEgU8awapJzKnqDKgw"
  val RsaThumbprint = "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs"

  val EcX = "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4"
  val EcY = "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM"

  val Ed25519X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
  val Ed25519Thumbprint = "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k"
}
