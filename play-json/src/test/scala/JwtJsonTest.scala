package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtJsonSpec extends UnitSpec with JsonFixture {
  describe("JwtJson") {
    it("should encode") {
      dataJson foreach { d =>
        assertResult(d.token, d.algo.fullName + " Option(key)") {
          JwtJson.encode(d.headerJson, claimJson, secretKey)
        }

        assertResult(d.token, d.algo.fullName + " String key") {
          JwtJson.encode(d.headerJson, claimJson, secretKey.get)
        }

        assertResult(d.tokenUnsigned, d.algo.fullName + " No key") {
          JwtJson.encode(d.headerJson, claimJson)
        }
      }
    }

    it("should decodeAllJson when now is before expiration date") {
      val mock = mockBeforeExpiration
      dataJson foreach { d =>
        val success = Success(d.headerJson, claimJson, Option(d.signature))
        assertResult(success, d.algo.fullName) { JwtJson.decodeAllJson(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should fail to decodeAllJson when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d => assert(JwtJson.decodeAllJson(d.token, secretKey).isFailure) }
      mock.tearDown
    }

    it("should decodeJson when now is before expiration date") {
      val mock = mockBeforeExpiration
      val success = Success(claimJson)
      dataJson foreach { d =>
        assertResult(success, d.algo.fullName + " Option(key)") { JwtJson.decodeJson(d.token, secretKey) }
        assertResult(success, d.algo.fullName + " String key") { JwtJson.decodeJson(d.token, secretKey.get) }
      }
      mock.tearDown
    }

    it("should fail to decodeJson when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d =>
        assert(JwtJson.decodeJson(d.token, secretKey).isFailure)
        assert(JwtJson.decodeJson(d.token, secretKey.get).isFailure)
      }
      mock.tearDown
    }

    it("should decodeAll when now is before expiration date") {
      val mock = mockBeforeExpiration
      dataJson foreach { d =>
        val success = Success(d.headerClass, claimClass, Option(d.signature))
        assertResult(success, d.algo.fullName) { JwtJson.decodeAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should fail to decodeAll when now is after expiration date") {
      val mock = mockAfterExpiration
      dataJson foreach { d => assert(JwtJson.decodeAll(d.token, secretKey).isFailure) }
      mock.tearDown
    }
  }
}
