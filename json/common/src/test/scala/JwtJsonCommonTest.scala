package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

abstract class JwtJsonCommonSpec[J] extends UnitSpec with JsonCommonFixture[J] {
  def jwtJsonCommon: JwtJsonCommon[J]

  def battleTestEncode(d: JsonDataEntryTrait[J], key: String) = {
    assertResult(d.token, d.algo.fullName + " Option(key)") { jwtJsonCommon.encode(d.headerJson, claimJson, Option(key)) }
    assertResult(d.token, d.algo.fullName + " String key") { jwtJsonCommon.encode(d.headerJson, claimJson, key) }
    assertResult(d.tokenUnsigned, d.algo.fullName + " No key") { jwtJsonCommon.encode(d.headerJson, claimJson) }
    assertResult(d.tokenEmpty, d.algo.fullName + " No header, None, None") { jwtJsonCommon.encode(claimJson, None, None) }
    assertResult(d.token, d.algo.fullName + " No header, Option(key)") { jwtJsonCommon.encode(claimJson, Option(key), Option(d.algo)) }
    assertResult(d.token, d.algo.fullName + " No header, String key") { jwtJsonCommon.encode(claimJson, key, d.algo) }
    assertResult(d.tokenEmpty, d.algo.fullName + " No header, No key") { jwtJsonCommon.encode(claimJson) }
  }

  describe("JwtJson") {
    it("should encode HMAC") {
      dataJson foreach { d => battleTestEncode(d, secretKey) }
    }

    it("should encode RSA") {
      dataRSAJson foreach { d => battleTestEncode(d, privateKeyRSA) }
    }

    it("should decodeJsonAll when now is before expiration date") {
      val mock = mockBeforeExpiration
      dataJson foreach { d =>
        val success = Success(d.headerJson, claimJson, Option(d.signature))
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeJsonAll(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should fail to decodeJsonAll when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d => assert(jwtJsonCommon.decodeJsonAll(d.token, secretKeyOpt).isFailure) }
      mock.tearDown
    }

    it("should decodeJson when now is before expiration date") {
      val mock = mockBeforeExpiration
      val success = Success(claimJson)
      dataJson foreach { d =>
        assertResult(success, d.algo.fullName + " Option(key)") { jwtJsonCommon.decodeJson(d.token, secretKeyOpt) }
        assertResult(success, d.algo.fullName + " String key") { jwtJsonCommon.decodeJson(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should fail to decodeJson when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d =>
        assert(jwtJsonCommon.decodeJson(d.token, secretKeyOpt).isFailure)
        assert(jwtJsonCommon.decodeJson(d.token, secretKey).isFailure)
      }
      mock.tearDown
    }

    it("should decodeAll when now is before expiration date") {
      val mock = mockBeforeExpiration
      dataJson foreach { d =>
        val success = Success(d.headerClass, claimClass, Option(d.signature))
        assertResult(success, d.algo.fullName) { jwtJsonCommon.decodeAll(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should fail to decodeAll when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d => assert(jwtJsonCommon.decodeAll(d.token, secretKeyOpt).isFailure) }
      mock.tearDown
    }
  }
}
