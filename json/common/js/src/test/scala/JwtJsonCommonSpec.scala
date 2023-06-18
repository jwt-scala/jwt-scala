package pdi.jwt

import java.time.Clock
import scala.util.Success

import pdi.jwt.exceptions.*

abstract class JwtJsonCommonSpec[J] extends munit.FunSuite with JsonCommonFixture[J] {
  import JwtJsonCommonSpec.JwtJsonUnderTest

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
    }

    dataRSAJson.foreach { d =>
      assertEquals(
        validTimeJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        success,
        d.algo.fullName
      )
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
      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac()).get
      }
      assert(
        afterExpirationJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure
      )

      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac()).get
      }
      assert(afterExpirationJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure)

      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac()).get
      }
      assert(afterExpirationJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac()).isFailure)
    }

    dataRSAJson.foreach { d =>
      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).get
      }
      assert(
        afterExpirationJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure
      )

      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).get
      }
      assert(
        afterExpirationJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure
      )

      intercept[JwtExpirationException] {
        afterExpirationJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).get
      }
      assert(afterExpirationJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA()).isFailure)
    }
  }

  test(
    "should success to decodeJsonAll and decodeJson when now is after expiration date with options"
  ) {
    val options = JwtOptions(expiration = false)

    dataJson.foreach { d =>
      afterExpirationJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac(), options).get
      assert(
        afterExpirationJwt
          .decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )

      afterExpirationJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac(), options).get
      assert(
        afterExpirationJwt
          .decodeJson(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )

      afterExpirationJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac(), options).get
      assert(
        afterExpirationJwt
          .decodeAll(d.token, secretKey, JwtAlgorithm.allHmac(), options)
          .isSuccess
      )
    }

    dataRSAJson.foreach { d =>
      afterExpirationJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options).get
      assert(
        afterExpirationJwt
          .decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )

      afterExpirationJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options).get
      assert(
        afterExpirationJwt
          .decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )

      afterExpirationJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options).get
      assert(
        afterExpirationJwt
          .decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
          .isSuccess
      )
    }
  }
}

object JwtJsonCommonSpec {
  type JwtJsonUnderTest[J] = JwtJsonCommon[J, JwtHeader, JwtClaim]
}
