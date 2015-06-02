package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec with Fixture {
  def battleTestEncode(d: DataEntryBase, key: String) = {
    assertResult(d.tokenEmpty, d.algo.fullName) { Jwt.encode(claim) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(d.header, claim, key, d.algo) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claim, key, d.algo) }
    assertResult(d.tokenEmpty, d.algo.fullName) { Jwt.encode(claimClass) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(claimClass, key, d.algo) }
    assertResult(d.token, d.algo.fullName) { Jwt.encode(d.headerClass, claimClass, key) }
  }

  describe("Jwt") {
    it("should encode Hmac") {
      val mock = mockValidTime
      data foreach { d => battleTestEncode(d, secretKey) }
      mock.tearDown
    }

    it("should encode RSA") {
      val mock = mockValidTime
      dataRSA foreach { d => battleTestEncode(d, privateKeyRSA) }
      mock.tearDown
    }

    it("should be symetric") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult((d.header, claim, d.signature), d.algo.fullName) {
          Jwt.decodeAll(Jwt.encode(d.header, claim, secretKey, d.algo), secretKey).get
        }
      }

      dataRSA foreach { d =>
        assert({
          val (h, c, s) = Jwt.decodeAll(Jwt.encode(d.header, claim, randomRSAKey.getPrivate, d.algo.asInstanceOf[JwtRSAAlgorithm]), randomRSAKey.getPublic).get

          h == d.header && c == claim
        }, d.algo.fullName)
      }

      dataECDSA foreach { d =>
        assert({
          val (h, c, s) = Jwt.decodeAll(Jwt.encode(d.header, claim, randomECKey.getPrivate, d.algo.asInstanceOf[JwtECDSAAlgorithm]), randomECKey.getPublic).get

          h == d.header && c == claim
        }, d.algo.fullName)
      }

      mock.tearDown
    }

    it("should decodeRawAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeRaw") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeAll(d.token, secretKey) }
      }
      mock.tearDown
    }

    it("should decode") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKey) }
      }
      mock.tearDown
    }

    def battleTestValidation(d: DataEntryBase, key: String) = {
      assertResult((), d.algo.fullName) { Jwt.validate(d.token, key) }
      assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, key) }
    }

    it("should validate correct tokens") {
      val mock = mockValidTime
      data foreach { d => assertResult((), d.algo.fullName) { battleTestValidation(d, secretKey) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { battleTestValidation(d, publicKeyRSA) } }
      mock.tearDown
    }

    it("should invalidate WTF tokens") {
      val tokens = Seq("1", "abcde", "", "a.b.c.d")

      tokens.foreach { token =>
        intercept[JwtLengthException] { Jwt.validate(token, secretKey) }
        assertResult(false, token) { Jwt.isValid(token, secretKey) }
      }
    }

    it("should invalidate non-base64 tokens") {
      val tokens = Seq("a.b", "a.b.c", "1.2.3", "abcde.azer.azer", "aze$.azer.azer")

      tokens.foreach { token =>
        intercept[IllegalArgumentException] { Jwt.validate(token, secretKey) }
        assertResult(false, token) { Jwt.isValid(token, secretKey) }
      }
    }

    def battleTestExpirationValidation(d: DataEntryBase, key: String) = {
      intercept[JwtExpirationException] { Jwt.validate(d.token, key) }
      assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, key) }
    }

    it("should invalidate expired tokens") {
      val mock = mockAfterExpiration
      data foreach { d => assertResult((), d.algo.fullName) { battleTestExpirationValidation(d, secretKey) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { battleTestExpirationValidation(d, publicKeyRSA) } }
      mock.tearDown
    }

    def battleTestEarlyValidation(d: DataEntry, keyPrivate: String, keyPublic: String) = {
      val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
      val token = Jwt.encode(claimNotBefore, keyPrivate, d.algo)

      intercept[JwtNotBeforeException] { Jwt.validate(token, keyPublic) }
      assertResult(false, d.algo.fullName) { Jwt.isValid(token, keyPublic) }
    }

    it("should invalidate early tokens") {
      val mock = mockBeforeNotBefore
      data foreach { d => assertResult((), d.algo.fullName) { battleTestEarlyValidation(d, secretKey, secretKey) } }
      dataRSA foreach { d => assertResult((), d.algo.fullName) { battleTestEarlyValidation(d, privateKeyRSA, publicKeyRSA) } }
      mock.tearDown
    }

    def battleTestKeyValidation(d: DataEntryBase, key: String) = {
      assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, key) }
    }

    it("should invalidate wrong keys") {
      val mock = mockValidTime

      data foreach { d => assertResult((), d.algo.fullName) {
        intercept[JwtValidationException] { Jwt.validate(d.token, "wrong key") }
        battleTestKeyValidation(d, "wrong key") }
      }

      dataRSA foreach { d => assertResult((), d.algo.fullName) {
        battleTestKeyValidation(d, "wrong key") }
      }

      mock.tearDown
    }

    it("should invalidate wrong algos") {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
      assert(Jwt.decode(token).isFailure)
      intercept[JwtNonSupportedAlgorithm] { Jwt.decode(token).get }
    }
  }
}
