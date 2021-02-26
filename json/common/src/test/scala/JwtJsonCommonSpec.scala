package pdi.jwt

import java.time.Clock
import pdi.jwt.exceptions._
import scala.util.Success

abstract class JwtJsonCommonSpec[J] extends UnitSpec with JsonCommonFixture[J] {
  import JwtJsonCommonSpec.JwtJsonUnderTest

  protected def jwtJsonCommon(clock: Clock): JwtJsonUnderTest[J]

  protected def testEncoding: Boolean = true

  lazy val defaultJwt: JwtJsonUnderTest[J] = jwtJsonCommon(Clock.systemUTC)
  lazy val afterExpirationJwt: JwtJsonUnderTest[J] = jwtJsonCommon(afterExpirationClock)
  lazy val validTimeJwt: JwtJsonUnderTest[J] = jwtJsonCommon(validTimeClock)

  def battleTestEncode(d: JsonDataEntryTrait[J], key: String, jwt: JwtJsonUnderTest[J]) = {
    assertResult(d.token, d.algo.fullName + " key") { jwt.encode(d.headerJson, claimJson, key) }
    assertResult(d.tokenEmpty, d.algo.fullName + " No header, no key, no algo") {
      jwt.encode(claimJson)
    }
    assertResult(d.token, d.algo.fullName + " No header, key, algo") {
      jwt.encode(claimJson, key, d.algo)
    }
  }

  describe("JwtJson") {
    if (testEncoding) {
      it("should encode with no algorithm") {
        assertResult(tokenEmpty, "Unsigned key") { defaultJwt.encode(headerEmptyJson, claimJson) }
      }

      it("should encode HMAC") {
        dataJson foreach { d => battleTestEncode(d, secretKey, defaultJwt) }
      }

      it("should encode RSA") {
        dataRSAJson foreach { d => battleTestEncode(d, privateKeyRSA, defaultJwt) }
      }
    }

    it("should decodeJsonAll") {
      dataJson foreach { d =>
        val success = Success((d.headerJson, claimJson, d.signature))
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJsonAll(d.token, secretKeyOf(d.algo))
        }
      }

      dataRSAJson foreach { d =>
        val success = Success((d.headerJson, claimJson, d.signature))
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should decodeJson") {
      val success = Success(claimJson)

      dataJson foreach { d =>
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJson(d.token, secretKeyOf(d.algo))
        }
      }

      dataRSAJson foreach { d =>
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should decodeAll") {
      dataJson foreach { d =>
        val success = Success((d.headerClass, claimClass, d.signature))
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeAll(d.token, secretKeyOf(d.algo))
        }
      }

      dataRSAJson foreach { d =>
        val success = Success((d.headerClass, claimClass, d.signature))
        assertResult(success, d.algo.fullName) {
          validTimeJwt.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should fail to decodeJsonAll and decodeJson when now is after expiration date") {
      dataJson foreach { d =>
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

      dataRSAJson foreach { d =>
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

    it(
      "should success to decodeJsonAll and decodeJson when now is after expiration date with options"
    ) {
      val options = JwtOptions(expiration = false)

      dataJson foreach { d =>
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

      dataRSAJson foreach { d =>
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
}

object JwtJsonCommonSpec {
  type JwtJsonUnderTest[J] = JwtJsonCommon[J, JwtHeader, JwtClaim]
}
