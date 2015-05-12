package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec with Fixture {
  describe("Jwt") {
    it("should encode Hmac") {
      data foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, secretKey.get, d.algo) }
      }
    }

    it("should encode RSA") {
      dataRSA foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, privateKeyRSA.get, d.algo) }
      }
    }

    it("should be symetric") {
      data foreach { d =>
        assertResult((d.header, claim, Option(d.signature)), d.algo.fullName) {
          Jwt.decodeAll(Jwt.encode(d.header, claim, secretKey.get, d.algo), secretKey).get
        }
      }
    }

    it("should encode case class") {
      data foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass, secretKey.get) }
      }
    }

    it("should decodeRawAll") {
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKey) }
      }
    }

    it("should decodeRaw") {
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKey) }
      }
    }

    it("should decodeAll") {
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeAll(d.token, secretKey) }
      }
    }

    it("should decode") {
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKey) }
      }
    }

    it("should validate") {
      data foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, secretKey) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, publicKeyRSA) } }
    }
  }
}
