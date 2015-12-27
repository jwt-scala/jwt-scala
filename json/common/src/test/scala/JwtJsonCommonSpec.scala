package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

import pdi.jwt.exceptions._

abstract class JwtJsonCommonSpec[J] extends UnitSpec with JsonCommonFixture[J] {
  def jwtJsonCommon: JwtJsonCommon[J]

  def battleTestEncode(d: JsonDataEntryTrait[J], key: String) = {
    assertResult(d.token, d.algo.fullName + " key") { jwtJsonCommon.encode(d.headerJson, claimJson, key) }
    assertResult(d.tokenEmpty, d.algo.fullName + " No header, no key, no algo") { jwtJsonCommon.encode(claimJson) }
    assertResult(d.token, d.algo.fullName + " No header, key, algo") { jwtJsonCommon.encode(claimJson, key, d.algo) }
  }

  describe("JwtJson") {
    it("should encode with no algoritm") {
      assertResult(tokenEmpty, "Unsigned key") { jwtJsonCommon.encode(headerEmptyJson, claimJson) }
    }

    it("should encode HMAC") {
      dataJson foreach { d => battleTestEncode(d, secretKey) }
    }

    it("should encode RSA") {
      dataRSAJson foreach { d => battleTestEncode(d, privateKeyRSA) }
    }

    it("should decodeJsonAll") {
      val mock = mockValidTime

      dataJson foreach { d =>
        val success = Success(d.headerJson, claimJson, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJsonAll(d.token, secretKeyOf(d.algo)) }
      }

      dataRSAJson foreach { d =>
        val success = Success(d.headerJson, claimJson, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should decodeJson") {
      val mock = mockValidTime
      val success = Success(claimJson)

      dataJson foreach { d =>
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJson(d.token, secretKeyOf(d.algo)) }
      }

      dataRSAJson foreach { d =>
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockValidTime

      dataJson foreach { d =>
        val success = Success(d.headerClass, claimClass, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeAll(d.token, secretKeyOf(d.algo)) }
      }

      dataRSAJson foreach { d =>
        val success = Success(d.headerClass, claimClass, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should fail to decodeJsonAll and decodeJson when now is after expiration date") {
      val mock = mockAfterExpiration

      dataJson foreach { d =>
        intercept[JwtExpirationException] { jwtJsonCommon.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac).get }
        assert(jwtJsonCommon.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac).isFailure)

        intercept[JwtExpirationException] { jwtJsonCommon.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac).get }
        assert(jwtJsonCommon.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac).isFailure)

        intercept[JwtExpirationException] { jwtJsonCommon.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac).get }
        assert(jwtJsonCommon.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac).isFailure)
      }

      dataRSAJson foreach { d =>
        intercept[JwtExpirationException] { jwtJsonCommon.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA).get }
        assert(jwtJsonCommon.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA).isFailure)

        intercept[JwtExpirationException] { jwtJsonCommon.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA).get }
        assert(jwtJsonCommon.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA).isFailure)

        intercept[JwtExpirationException] { jwtJsonCommon.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA).get }
        assert(jwtJsonCommon.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA).isFailure)
      }

      mock.tearDown
    }

    it("should success to decodeJsonAll and decodeJson when now is after expiration date with options") {
      val mock = mockAfterExpiration
      val options = JwtOptions(expiration = false)

      dataJson foreach { d =>
        jwtJsonCommon.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac, options).get
        assert(jwtJsonCommon.decodeJsonAll(d.token, secretKey, JwtAlgorithm.allHmac, options).isSuccess)

        jwtJsonCommon.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac, options).get
        assert(jwtJsonCommon.decodeJson(d.token, secretKey, JwtAlgorithm.allHmac, options).isSuccess)

        jwtJsonCommon.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac, options).get
        assert(jwtJsonCommon.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac, options).isSuccess)
      }

      dataRSAJson foreach { d =>
        jwtJsonCommon.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).get
        assert(jwtJsonCommon.decodeJsonAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).isSuccess)

        jwtJsonCommon.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).get
        assert(jwtJsonCommon.decodeJson(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).isSuccess)

        jwtJsonCommon.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).get
        assert(jwtJsonCommon.decodeAll(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options).isSuccess)
      }

      mock.tearDown
    }
  }
}
