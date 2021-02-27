package pdi.jwt

import pdi.jwt.algorithms._
import pdi.jwt.exceptions._

import scala.util.Success

class JwtSpec extends UnitSpec with Fixture {
  val afterExpirationJwt: Jwt = Jwt(afterExpirationClock)
  val beforeNotBeforeJwt: Jwt = Jwt(beforeNotBeforeClock)
  val afterNotBeforeJwt: Jwt = Jwt(afterNotBeforeClock)
  val validTimeJwt: Jwt = Jwt(validTimeClock)

  def battleTestEncode(d: DataEntryBase, key: String, jwt: Jwt) = {
    assertResult(d.tokenEmpty, d.algo.fullName) { jwt.encode(claim) }
    assertResult(d.token, d.algo.fullName) { jwt.encode(d.header, claim, key, d.algo) }
    assertResult(d.token, d.algo.fullName) { jwt.encode(claim, key, d.algo) }
    assertResult(d.tokenEmpty, d.algo.fullName) { jwt.encode(claimClass) }
    assertResult(d.token, d.algo.fullName) { jwt.encode(claimClass, key, d.algo) }
    assertResult(d.token, d.algo.fullName) { jwt.encode(d.headerClass, claimClass, key) }
  }

  describe("Jwt") {
    it("should parse JSON with spaces") {
      assertResult(true) { Jwt.isValid(tokenWithSpaces) }
    }

    it("should encode Hmac") {
      data foreach { d => battleTestEncode(d, secretKey, validTimeJwt) }
    }

    it("should encode RSA") {
      dataRSA foreach { d => battleTestEncode(d, privateKeyRSA, validTimeJwt) }
    }

    it("should be symmetric") {
      data foreach { d =>
        testTryAll(
          validTimeJwt.decodeAll(
            validTimeJwt.encode(d.header, claim, secretKey, d.algo),
            secretKey,
            JwtAlgorithm.allHmac()
          ),
          (d.headerClass, claimClass, d.signature),
          d.algo.fullName
        )
      }
    }

    it("should be symmetric (RSA)") {
      dataRSA foreach { d =>
        testTryAllWithoutSignature(
          validTimeJwt.decodeAll(
            validTimeJwt.encode(
              d.header,
              claim,
              randomRSAKey.getPrivate,
              d.algo.asInstanceOf[JwtRSAAlgorithm]
            ),
            randomRSAKey.getPublic,
            JwtAlgorithm.allRSA()
          ),
          (d.headerClass, claimClass),
          d.algo.fullName
        )

        testTryAllWithoutSignature(
          validTimeJwt.decodeAll(
            validTimeJwt.encode(
              d.header,
              claim,
              randomRSAKey.getPrivate,
              d.algo.asInstanceOf[JwtRSAAlgorithm]
            ),
            randomRSAKey.getPublic
          ),
          (d.headerClass, claimClass),
          d.algo.fullName
        )
      }
    }

    it("should be symmetric (ECDSA)") {
      dataECDSA foreach { d =>
        testTryAllWithoutSignature(
          validTimeJwt.decodeAll(
            validTimeJwt.encode(
              d.header,
              claim,
              randomECKey.getPrivate,
              d.algo.asInstanceOf[JwtECDSAAlgorithm]
            ),
            randomECKey.getPublic,
            JwtAlgorithm.allECDSA()
          ),
          (d.headerClass, claimClass),
          d.algo.fullName
        )
      }

    }

    it("should decodeRawAll") {
      data foreach { d =>
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) {
          validTimeJwt.decodeRawAll(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(Success((d.header, claim, d.signature)), d.algo.fullName) {
          validTimeJwt.decodeRawAll(d.token, secretKeyOf(d.algo))
        }
      }
    }

    it("should decodeRaw") {
      data foreach { d =>
        assertResult(Success((claim)), d.algo.fullName) {
          validTimeJwt.decodeRaw(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(Success((claim)), d.algo.fullName) {
          validTimeJwt.decodeRaw(d.token, secretKeyOf(d.algo))
        }
      }
    }

    it("should decodeAll") {
      data foreach { d =>
        testTryAll(
          validTimeJwt.decodeAll(d.token, secretKey, JwtAlgorithm.allHmac()),
          (d.headerClass, claimClass, d.signature),
          d.algo.fullName
        )

        testTryAll(
          validTimeJwt.decodeAll(d.token, secretKeyOf(d.algo)),
          (d.headerClass, claimClass, d.signature),
          d.algo.fullName
        )
      }
    }

    it("should decode") {
      data foreach { d =>
        testTryClaim(
          validTimeJwt.decode(d.token, secretKey, JwtAlgorithm.allHmac()),
          claimClass,
          d.algo.fullName
        )

        testTryClaim(
          validTimeJwt.decode(d.token, secretKeyOf(d.algo)),
          claimClass,
          d.algo.fullName
        )
      }
    }

    it("should validate correct tokens") {

      data foreach { d =>
        assertResult((), d.algo.fullName) {
          validTimeJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(true, d.algo.fullName) {
          validTimeJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult((), d.algo.fullName) { validTimeJwt.validate(d.token, secretKeyOf(d.algo)) }
        assertResult(true, d.algo.fullName) { validTimeJwt.isValid(d.token, secretKeyOf(d.algo)) }
      }

      dataRSA foreach { d =>
        assertResult((), d.algo.fullName) {
          validTimeJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
        assertResult(true, d.algo.fullName) {
          validTimeJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }

    }

    it("should validate ECDSA from other implementations") {
      val publicKey =
        "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQASisgweVL1tAtIvfmpoqvdXF8sPKTV9YTKNxBwkdkm+/auh4pR8TbaIfsEzcsGUVv61DFNFXb0ozJfurQ59G2XcgAn3vROlSSnpbIvuhKrzL5jwWDTaYa5tVF1Zjwia/5HUhKBkcPuWGXg05nMjWhZfCuEetzMLoGcHmtvabugFrqsAg="
      val verifier = (token: String) => {
        assertResult(true) {
          Jwt.isValid(token, publicKey, Seq(JwtAlgorithm.ES512))
        }
      }
      //Test verification for token created using https://github.com/auth0/node-jsonwebtoken/tree/v7.0.1
      verifier(
        "eyJhbGciOiJFUzUxMiIsInR5cCI6IkpXVCJ9.eyJ0ZXN0IjoidGVzdCIsImlhdCI6MTQ2NzA2NTgyN30.Aab4x7HNRzetjgZ88AMGdYV2Ml7kzFbl8Ql2zXvBores7iRqm2nK6810ANpVo5okhHa82MQf2Q_Zn4tFyLDR9z4GAcKFdcAtopxq1h8X58qBWgNOc0Bn40SsgUc8wOX4rFohUCzEtnUREePsvc9EfXjjAH78WD2nq4tn-N94vf14SncQ"
      )
      //Test verification for token created using https://github.com/jwt/ruby-jwt/tree/v1.5.4
      verifier(
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzUxMiJ9.eyJ0ZXN0IjoidGVzdCJ9.AV26tERbSEwcoDGshneZmhokg-tAKUk0uQBoHBohveEd51D5f6EIs6cskkgwtfzs4qAGfx2rYxqQXr7LTXCNquKiAJNkTIKVddbPfped3_TQtmHZTmMNiqmWjiFj7Y9eTPMMRRu26w4gD1a8EQcBF-7UGgeH4L_1CwHJWAXGbtu7uMUn"
      )
    }

    it("should invalidate WTF tokens") {
      val tokens = Seq("1", "abcde", "", "a.b.c.d")

      tokens.foreach { token =>
        intercept[JwtLengthException] { Jwt.validate(token, secretKey, JwtAlgorithm.allHmac()) }
        assertResult(false, token) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac()) }
      }
    }

    it("should invalidate non-base64 tokens") {
      val tokens = Seq("a.b", "a.b.c", "1.2.3", "abcde.azer.azer", "aze$.azer.azer")

      tokens.foreach { token =>
        intercept[IllegalArgumentException] {
          Jwt.validate(token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(false, token) { Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac()) }
      }
    }

    it("should invalidate expired tokens") {
      data foreach { d =>
        intercept[JwtExpirationException] {
          afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(false, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac())
        }
        intercept[JwtExpirationException] {
          afterExpirationJwt.validate(d.token, secretKeyOf(d.algo))
        }
        assertResult(false, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo))
        }
      }

      dataRSA foreach { d =>
        intercept[JwtExpirationException] {
          afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
        assertResult(false, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should validate expired tokens with leeway") {
      val options = JwtOptions(leeway = 60)

      data foreach { d =>
        afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac(), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac(), options)
        }
        afterExpirationJwt.validate(d.token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo), options)
        }
      }

      dataRSA foreach { d =>
        afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        }
      }
    }

    it("should invalidate early tokens") {
      data foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

        intercept[JwtNotBeforeException] {
          beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac())
        }
        assertResult(false, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac())
        }
        intercept[JwtNotBeforeException] { beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo)) }
        assertResult(false, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo))
        }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        intercept[JwtNotBeforeException] {
          beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
        assertResult(false, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should validate early tokens with leeway") {
      val options = JwtOptions(leeway = 60)

      data foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

        beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac(), options)
        assertResult(true, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac(), options)
        }
        beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo), options)
        }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        intercept[JwtNotBeforeException] {
          beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
        assertResult(false, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA())
        }
      }
    }

    it("should invalidate wrong keys") {
      data foreach { d =>
        intercept[JwtValidationException] {
          validTimeJwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac())
        }
        assertResult(false, d.algo.fullName) {
          validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac())
        }
      }

      dataRSA foreach { d =>
        assertResult(false, d.algo.fullName) {
          validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA())
        }
      }
    }

    it("should fail on non-exposed algorithms") {
      data foreach { d =>
        intercept[JwtValidationException] {
          validTimeJwt.validate(d.token, secretKey, Seq.empty[JwtHmacAlgorithm])
        }
        assertResult(false, d.algo.fullName) {
          validTimeJwt.isValid(d.token, secretKey, Seq.empty[JwtHmacAlgorithm])
        }
      }

      data foreach { d =>
        intercept[JwtValidationException] {
          validTimeJwt.validate(d.token, secretKey, JwtAlgorithm.allRSA())
        }
        assertResult(false, d.algo.fullName) {
          validTimeJwt.isValid(d.token, secretKey, JwtAlgorithm.allRSA())
        }
      }

      dataRSA foreach { d =>
        intercept[JwtValidationException] {
          validTimeJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allHmac())
        }
        assertResult(false, d.algo.fullName) {
          validTimeJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allHmac())
        }
      }
    }

    it("should invalidate wrong algos") {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
      assert(Jwt.decode(token).isFailure)
      intercept[JwtNonSupportedAlgorithm] { Jwt.decode(token).get }
    }

    it("should decode tokens with unknown algos depending on options") {
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
      val decoded = Jwt.decode(token, options = JwtOptions(signature = false))
      assert(decoded.isSuccess)
    }

    it("should skip expiration validation depending on options") {
      val options = JwtOptions(expiration = false)

      data foreach { d =>
        afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac(), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac(), options)
        }
        afterExpirationJwt.validate(d.token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo), options)
        }
      }

      dataRSA foreach { d =>
        afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        assertResult(true, d.algo.fullName) {
          afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        }
      }

    }

    it("should skip notBefore validation depending on options") {
      val options = JwtOptions(notBefore = false)

      data foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

        beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac(), options)
        assertResult(true, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac(), options)
        }
        beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo), options)
        assertResult(true, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo), options)
        }
      }

      dataRSA foreach { d =>
        val claimNotBefore = claimClass.startsAt(notBefore)
        val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

        beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        assertResult(true, d.algo.fullName) {
          beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
        }
      }
    }

    it("should skip signature validation depending on options") {
      val options = JwtOptions(signature = false)

      data foreach { d =>
        validTimeJwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac(), options)
        assertResult(true, d.algo.fullName) {
          validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac(), options)
        }
      }

      dataRSA foreach { d =>
        assertResult(true, d.algo.fullName) {
          validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA(), options)
        }
      }
    }
  }
}
