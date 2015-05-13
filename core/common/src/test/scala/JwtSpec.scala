package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec with Fixture {
  describe("Jwt") {
    it("should encode Hmac") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, secretKey.get, d.algo) }
      }
      mock.tearDown
    }

    it("should encode RSA") {
      val mock = mockBeforeExpiration
      dataRSA foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, privateKeyRSA.get, d.algo) }
      }
      mock.tearDown
    }

    it("should be symetric") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult((d.header, claim, Option(d.signature)), d.algo.fullName) {
          Jwt.decodeAll(Jwt.encode(d.header, claim, secretKey.get, d.algo), secretKey).get
        }
      }
      mock.tearDown
    }

    it("should encode case class") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass, secretKey.get) }
      }
      mock.tearDown
    }

    it("should decodeRawAll") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeRaw") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decode") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should validate") {
      val mock = mockBeforeExpiration
      data foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, secretKey) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, publicKeyRSA) } }
      mock.tearDown
    }
  }
}
