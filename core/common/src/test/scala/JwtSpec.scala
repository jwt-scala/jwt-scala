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
    it("should parse JSON with spaces") {
      assertResult(true) { Jwt.isValid(tokenWithSpaces) }
    }

    it("should encode Hmac") {
      val mock = mockValidTime
      data foreach { d => battleTestEncode(d, secretKey) }
      tearDown(mock)
    }

    it("should encode RSA") {
      val mock = mockValidTime
      dataRSA foreach { d => battleTestEncode(d, privateKeyRSA) }
      tearDown(mock)
    }

    it("should be symmetric") {
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

      tearDown(mock)
    }

    it("should decodeRawAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeRawAll(d.token, secretKeyOf(d.algo)) }
      }
      tearDown(mock)
    }

    it("should decodeRaw") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((claim)), d.algo.fullName) { Jwt.decodeRaw(d.token, secretKeyOf(d.algo)) }
      }
      tearDown(mock)
    }

    it("should decodeAll") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) { Jwt.decodeAll(d.token, secretKeyOf(d.algo)) }
      }
      tearDown(mock)
    }

    it("should decode") {
      val mock = mockValidTime
      data foreach { d =>
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKey, JwtAlgorithm.allHmac) }
        assertResult(Success(claim), d.algo.fullName) { Jwt.decode(d.token, secretKeyOf(d.algo)) }
      }
      tearDown(mock)
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

      tearDown(mock)
    }

    it("should validate ECDSA from other implementations") {
      val publicKey = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQASisgweVL1tAtIvfmpoqvdXF8sPKTV9YTKNxBwkdkm+/auh4pR8TbaIfsEzcsGUVv61DFNFXb0ozJfurQ59G2XcgAn3vROlSSnpbIvuhKrzL5jwWDTaYa5tVF1Zjwia/5HUhKBkcPuWGXg05nMjWhZfCuEetzMLoGcHmtvabugFrqsAg="
      val verifier = (token: String) => {
        assertResult(true) {
          Jwt.isValid(token, publicKey, Seq(JwtAlgorithm.ES512))
        }
      }
      //Test verification for token created using https://github.com/auth0/node-jsonwebtoken/tree/v7.0.1
      verifier("eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCJ9.eyJ0ZXN0IjoidGVzdCIsImlhdCI6MTQ2NzA2NTgyN30.Aab4x7HNRzetjgZ88AMGdYV2Ml7kzFbl8Ql2zXvBores7iRqm2nK6810ANpVo5okhHa82MQf2Q_Zn4tFyLDR9z4GAcKFdcAtopxq1h8X58qBWgNOc0Bn40SsgUc8wOX4rFohUCzEtnUREePsvc9EfXjjAH78WD2nq4tn-N94vf14SncQ")
      //Test verification for token created using https://github.com/jwt/ruby-jwt/tree/v1.5.4
      verifier("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzUxMiJ9.eyJ0ZXN0IjoidGVzdCJ9.AV26tERbSEwcoDGshneZmhokg-tAKUk0uQBoHBohveEd51D5f6EIs6cskkgwtfzs4qAGfx2rYxqQXr7LTXCNquKiAJNkTIKVddbPfped3_TQtmHZTmMNiqmWjiFj7Y9eTPMMRRu26w4gD1a8EQcBF-7UGgeH4L_1CwHJWAXGbtu7uMUn")
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
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

      tearDown(mock)
    }
  }
}
