package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec with Fixture {
  describe("Jwt") {
    it("should encode") {
      data foreach { d =>
        assertResult(d.token, d.algo) { Jwt.encode(d.header, claim, secretKey.get, d.algo) }
      }
    }

    it("should encode case class") {
      data foreach { d =>
        assertResult(d.token, d.algo) { Jwt.encode(d.headerClass, claimClass, secretKey.get) }
      }
    }

    it("should decodeRawAll") {
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo) { Jwt.decodeRawAll(d.token, secretKey) }
      }
    }

    it("should decodeRaw") {
      data foreach { d =>
        assertResult(Success((claim)), d.algo) { Jwt.decodeRaw(d.token, secretKey) }
      }
    }

    it("should decodeAll") {
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo) { Jwt.decodeAll(d.token, secretKey) }
      }
    }

    it("should decode") {
      data foreach { d =>
        assertResult(Success(claim), d.algo) { Jwt.decode(d.token, secretKey) }
      }
    }

    it("should validate") {
      // TODO
    }
  }
}
