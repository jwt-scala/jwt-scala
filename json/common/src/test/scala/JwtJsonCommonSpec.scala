package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

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

    it("should decodeJsonAll when now is before expiration date") {
      val mock = mockValidTime
      dataJson foreach { d =>
        val success = Success(d.headerJson, claimJson, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJsonAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeJson") {
      val mock = mockValidTime
      val success = Success(claimJson)
      dataJson foreach { d =>
        assertResult(success, d.algo.fullName + " String key") { jwtJsonCommon.decodeJson(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockValidTime
      dataJson foreach { d =>
        val success = Success(d.headerClass, claimClass, d.signature)
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    def battleTestExpirationValidation(d: JsonDataEntryTrait[J], key: String) = {
      intercept[JwtExpirationException] { jwtJsonCommon.decodeJsonAll(d.token, key).get }
      assert(jwtJsonCommon.decodeJsonAll(d.token, key).isFailure)

      intercept[JwtExpirationException] { jwtJsonCommon.decodeJson(d.token, key).get }
      assert(jwtJsonCommon.decodeJson(d.token, key).isFailure)

      intercept[JwtExpirationException] { jwtJsonCommon.decodeAll(d.token, key).get }
      assert(jwtJsonCommon.decodeAll(d.token, key).isFailure)
    }

    it("should fail to decodeJsonAll and decodeJson when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d => battleTestExpirationValidation(d, secretKey) }
      dataRSAJson foreach { d => battleTestExpirationValidation(d, publicKeyRSA) }
      mock.tearDown
    }
  }
}
