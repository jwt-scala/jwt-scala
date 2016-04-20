package pdi.jwt

import org.scalatest._

import scala.util.{Success, Failure}

import pdi.jwt.algorithms._
import pdi.jwt.exceptions._

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
          Jwt.decodeAll(Jwt.encode(d.header, claim, secretKey, d.algo), secretKey, JwtAlgorithm.allHmac).get
        }
      }

      dataRSA foreach { d =>
        assert({
          val (h, c, s) = Jwt.decodeAll(Jwt.encode(d.header, claim, randomRSAKey.getPrivate, d.algo.asInstanceOf[JwtRSAAlgorithm]), randomRSAKey.getPublic, JwtAlgorithm.allRSA).get

          h == d.header && c == claim
        }, d.algo.fullName)

        assert({
          val (h, c, s) = Jwt.decodeAll(Jwt.encode(d.header, claim, randomRSAKey.getPrivate, d.algo.asInstanceOf[JwtRSAAlgorithm]), randomRSAKey.getPublic).get

          h == d.header && c == claim
        }, d.algo.fullName)
      }

      dataECDSA foreach { d =>
        assert({
          val (h, c, s) = Jwt.decodeAll(Jwt.encode(d.header, claim, randomECKey.getPrivate, d.algo.asInstanceOf[JwtECDSAAlgorithm]), randomECKey.getPublic, JwtAlgorithm.allECDSA).get

          h == d.header && c == claim
        }, d.algo.fullName)
      }

      mock.tearDown
    }

    it("should decodeRawAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKeyOf(d.algo)) }
      }
      mock.tearDown
    }

    it("should decodeRaw") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKeyOf(d.algo)) }
      }
      mock.tearDown
    }

    it("should decodeAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeAll(d.token, secretKeyOf(d.algo)) }
      }
      mock.tearDown
    }

    it("should decode") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKeyOf(d.algo)) }
      }
      mock.tearDown
    }

    it("should validate correct tokens") {
      val mock = mockValidTime

      data foreach { d =>
        assertResult((), d.algo.fullName) { Jwt.validate(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult((), d.algo.fullName) { Jwt.validate(d.token, secretKeyOf(d.algo)) }
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKeyOf(d.algo)) }
      }

      dataRSA foreach { d =>
        assertResult((), d.algo.fullName) { Jwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should invalidate WTF tokens") {
      val tokens = Seq("1", "abcde", "", "a.b.c.d")

      tokens.foreach { token =>
        intercept[JwtLengthException] { Jwt.validate(token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(false, token) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac) }
      }
    }

    it("should invalidate non-base64 tokens") {
      val tokens = Seq("a.b", "a.b.c", "1.2.3", "abcde.azer.azer", "aze$.azer.azer")

      tokens.foreach { token =>
        intercept[IllegalArgumentException] { Jwt.validate(token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(false, token) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac) }
      }
    }

    it("should invalidate expired tokens") {
      val mock = mockAfterExpiration

      data foreach { d =>
        intercept[JwtExpirationException] { Jwt.validate(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac) }
        intercept[JwtExpirationException] { Jwt.validate(d.token, secretKeyOf(d.algo)) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, secretKeyOf(d.algo)) }
      }

      dataRSA foreach { d =>
        intercept[JwtExpirationException] { Jwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should validate expired tokens with leeway") {
      val mock = mockAfterExpiration
      val options = JwtOptions(leeway = 60)

      data foreach { d =>
        Jwt.validate(d.token, secretKey, JwtAlgorithm.allHmac, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac, options) }
        Jwt.validate(d.token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKeyOf(d.algo), options) }
      }

      dataRSA foreach { d =>
        Jwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options) }
      }

      mock.tearDown
    }

    it("should invalidate early tokens") {
      val mock = mockBeforeNotBefore

      data foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, secretKey, d.algo)

        intercept[JwtNotBeforeException] { Jwt.validate(token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac) }
        intercept[JwtNotBeforeException] { Jwt.validate(token, secretKeyOf(d.algo)) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(token, secretKeyOf(d.algo)) }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        intercept[JwtNotBeforeException] { Jwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should validate early tokens with leeway") {
      val mock = mockBeforeNotBefore
      val options = JwtOptions(leeway = 60)

      data foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, secretKey, d.algo)

        Jwt.validate(token, secretKey, JwtAlgorithm.allHmac, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac, options) }
        Jwt.validate(token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(token, secretKeyOf(d.algo), options) }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        intercept[JwtNotBeforeException] { Jwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should invalidate wrong keys") {
      val mock = mockValidTime

      data foreach { d =>
        intercept[JwtValidationException] { Jwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac) }
      }

      dataRSA foreach { d =>
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA) }
      }

      mock.tearDown
    }

    it("should fail on non-exposed algorithms") {
      val mock = mockValidTime

      data foreach { d =>
        intercept[JwtValidationException] { Jwt.validate(d.token, secretKey, Seq.empty[JwtHmacAlgorithm]) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, secretKey, Seq.empty[JwtHmacAlgorithm]) }
      }

      data foreach { d =>
        intercept[JwtValidationException] { Jwt.validate(d.token, secretKey, JwtAlgorithm.allRSA) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, secretKey, JwtAlgorithm.allRSA) }
      }

      dataRSA foreach { d =>
        intercept[JwtValidationException] { Jwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allHmac) }
        assertResult(false, d.algo.fullName) { Jwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allHmac) }
      }

      mock.tearDown
    }

    it("should invalidate wrong algos") {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
      assert(Jwt.decode(token).isFailure)
      intercept[JwtNonSupportedAlgorithm] { Jwt.decode(token).get }
    }

    it("should skip expiration validation depending on options") {
      val mock = mockAfterExpiration
      val options = JwtOptions(expiration = false)

      data foreach { d =>
        Jwt.validate(d.token, secretKey, JwtAlgorithm.allHmac, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac, options) }
        Jwt.validate(d.token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, secretKeyOf(d.algo), options) }
      }

      dataRSA foreach { d =>
        Jwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA, options) }
      }

      mock.tearDown
    }

    it("should skip notBefore validation depending on options") {
      val mock = mockBeforeNotBefore
      val options = JwtOptions(notBefore = false)

      data foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, secretKey, d.algo)

        Jwt.validate(token, secretKey, JwtAlgorithm.allHmac, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac, options) }
        Jwt.validate(token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(token, secretKeyOf(d.algo), options) }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.copy(notBefore = Option(notBefore))
        val token = Jwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        Jwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA, options) }
      }

      mock.tearDown
    }

    it("should skip signature validation depending on options") {
      val mock = mockValidTime
      val options = JwtOptions(signature = false)

      data foreach { d =>
        Jwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac, options)
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac, options) }
      }

      dataRSA foreach { d =>
        assertResult(true, d.algo.fullName) { Jwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA, options) }
      }

      mock.tearDown
    }
  }
}
