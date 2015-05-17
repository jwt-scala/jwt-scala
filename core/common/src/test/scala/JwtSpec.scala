package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec with Fixture {
  def battleTestEncode(d: DataEntryBase, key: String) = {
    assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, Option(key), Option(d.algo)) }
    assertResult(d.tokenEmpty, d.algo.fullName) { Jwt.encode(claim) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claim, Option(key), Option(d.algo)) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claim, key, d.algo) }
    assertResult(d.tokenEmpty, d.algo.fullName) { Jwt.encode(claimClass) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claimClass, Option(key), Option(d.algo)) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claimClass, key, d.algo) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, key, d.algo) }
    assertResult(d.tokenUnsigned, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass, key) }
  }

  describe("Jwt") {
    it("should encode Hmac") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        battleTestEncode(d, secretKey)
      }
      mock.tearDown
    }

    it("should encode RSA") {
      val mock = mockBeforeExpiration
      dataRSA foreach { d =>
        battleTestEncode(d, privateKeyRSA)
      }
      mock.tearDown
    }

    it("should be symetric") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult((d.header, claim, Option(d.signature)), d.algo.fullName) {
          Jwt.decodeAll(Jwt.encode(d.header, claim, secretKey, d.algo), secretKey).get
        }
      }
      mock.tearDown
    }

    it("should encode case class") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(d.token, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeRawAll") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should decodeRaw") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success((d.header, claim, Some(d.signature))), d.algo.fullName) { Jwt.decodeAll(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should decode") {
      val mock = mockBeforeExpiration
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKeyOpt) }
      }
      mock.tearDown
    }

    it("should validate") {
      val mock = mockBeforeExpiration
      data foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, secretKeyOpt) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { Jwt.validate(d.token, publicKeyRSA) } }
      mock.tearDown
    }
  }
}
