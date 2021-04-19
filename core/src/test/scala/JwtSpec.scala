package pdi.jwt

import pdi.jwt.algorithms._
import pdi.jwt.exceptions._

import scala.util.Success
import scala.util.Try

class JwtSpec extends munit.FunSuite with Fixture {
  val afterExpirationJwt: Jwt = Jwt(afterExpirationClock)
  val beforeNotBeforeJwt: Jwt = Jwt(beforeNotBeforeClock)
  val afterNotBeforeJwt: Jwt = Jwt(afterNotBeforeClock)
  val validTimeJwt: Jwt = Jwt(validTimeClock)

  def battleTestEncode(d: DataEntryBase, key: String, jwt: Jwt) = {
    assertEquals(d.tokenEmpty, jwt.encode(claim))
    assertEquals(d.token, jwt.encode(d.header, claim, key, d.algo))
    assertEquals(d.token, jwt.encode(claim, key, d.algo))
    assertEquals(d.tokenEmpty, jwt.encode(claimClass))
    assertEquals(d.token, jwt.encode(claimClass, key, d.algo))
    assertEquals(d.token, jwt.encode(d.headerClass, claimClass, key))
  }

  test("should parse JSON with spaces") {
    assert(Jwt.isValid(tokenWithSpaces))
  }

  test("should encode Hmac") {
    data foreach { d => battleTestEncode(d, secretKey, validTimeJwt) }
  }

  test("should encode RSA") {
    dataRSA foreach { d => battleTestEncode(d, privateKeyRSA, validTimeJwt) }
  }

  test("should encode Ed25519") {
    dataEdDSA foreach { d => battleTestEncode(d, privateKeyEd25519, validTimeJwt) }
  }

  test("should be symmetric") {
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

  test("should be symmetric (RSA)") {
    dataRSA foreach { d =>
      testTryAllWithoutSignature(
        validTimeJwt.decodeAll(
          validTimeJwt.encode(d.header, claim, randomRSAKey.getPrivate, d.algo),
          randomRSAKey.getPublic,
          JwtAlgorithm.allRSA()
        ),
        (d.headerClass, claimClass),
        d.algo.fullName
      )

      testTryAllWithoutSignature(
        validTimeJwt.decodeAll(
          validTimeJwt.encode(d.header, claim, randomRSAKey.getPrivate, d.algo),
          randomRSAKey.getPublic
        ),
        (d.headerClass, claimClass),
        d.algo.fullName
      )
    }
  }

  test("should be symmetric (ECDSA)") {
    dataECDSA foreach { d =>
      testTryAllWithoutSignature(
        validTimeJwt.decodeAll(
          validTimeJwt.encode(d.header, claim, randomECKey.getPrivate, d.algo),
          randomECKey.getPublic,
          JwtAlgorithm.allECDSA()
        ),
        (d.headerClass, claimClass),
        d.algo.fullName
      )
    }

  }

  test("should be symmetric (EdDSA)") {
    dataEdDSA foreach { d =>
      testTryAllWithoutSignature(
        validTimeJwt.decodeAll(
          validTimeJwt.encode(d.header, claim, randomEd25519Key.getPrivate, d.algo),
          randomEd25519Key.getPublic,
          JwtAlgorithm.allEdDSA()
        ),
        (d.headerClass, claimClass),
        d.algo.fullName
      )

      testTryAllWithoutSignature(
        validTimeJwt.decodeAll(
          validTimeJwt.encode(d.header, claim, randomEd25519Key.getPrivate, d.algo),
          randomEd25519Key.getPublic
        ),
        (d.headerClass, claimClass),
        d.algo.fullName
      )
    }
  }

  test("should decodeRawAll") {
    data foreach { d =>
      assertEquals(
        validTimeJwt.decodeRawAll(d.token, secretKey, JwtAlgorithm.allHmac()),
        Success((d.header, claim, d.signature)),
        d.algo.fullName
      )
      assertEquals(
        validTimeJwt.decodeRawAll(d.token, secretKeyOf(d.algo)),
        Success((d.header, claim, d.signature)),
        d.algo.fullName
      )
    }
  }

  test("should decodeRaw") {
    data foreach { d =>
      assertEquals(
        validTimeJwt.decodeRaw(d.token, secretKey, JwtAlgorithm.allHmac()),
        Success((claim)),
        d.algo.fullName
      )
      assertEquals(
        validTimeJwt.decodeRaw(d.token, secretKeyOf(d.algo)),
        Success((claim)),
        d.algo.fullName
      )
    }
  }

  test("should decodeAll") {
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

  test("should decode") {
    data foreach { d =>
      assertEquals(
        validTimeJwt.decode(d.token, secretKey, JwtAlgorithm.allHmac()).get,
        claimClass,
        d.algo.fullName
      )

      assertEquals(
        validTimeJwt.decode(d.token, secretKeyOf(d.algo)).get,
        claimClass,
        d.algo.fullName
      )
    }
  }

  test("should validate correct tokens") {

    data foreach { d =>
      assertEquals(
        (),
        validTimeJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac()),
        d.algo.fullName
      )
      assert(validTimeJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac()), d.algo.fullName)
      assertEquals((), validTimeJwt.validate(d.token, secretKeyOf(d.algo)), d.algo.fullName)
      assert(validTimeJwt.isValid(d.token, secretKeyOf(d.algo)), d.algo.fullName)
    }

    dataRSA foreach { d =>
      assertEquals(
        (),
        validTimeJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        d.algo.fullName
      )
      assert(validTimeJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA()), d.algo.fullName)
    }

    dataEdDSA foreach { d =>
      assertEquals(
        (),
        validTimeJwt.validate(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA()),
        d.algo.fullName
      )
      assert(
        validTimeJwt.isValid(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA()),
        d.algo.fullName
      )
    }
  }

  test("should validate ECDSA from other implementations") {
    val publicKey =
      "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQASisgweVL1tAtIvfmpoqvdXF8sPKTV9YTKNxBwkdkm+/auh4pR8TbaIfsEzcsGUVv61DFNFXb0ozJfurQ59G2XcgAn3vROlSSnpbIvuhKrzL5jwWDTaYa5tVF1Zjwia/5HUhKBkcPuWGXg05nMjWhZfCuEetzMLoGcHmtvabugFrqsAg="
    val verifier = (token: String) => {
      assert(Jwt.isValid(token, publicKey, Seq(JwtAlgorithm.ES512)))
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

  test("should invalidate WTF tokens") {
    val tokens = Seq("1", "abcde", "", "a.b.c.d")

    tokens.foreach { token =>
      intercept[JwtLengthException] { Jwt.validate(token, secretKey, JwtAlgorithm.allHmac()) }
      assert(!Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac()), token)
    }
  }

  test("should invalidate non-base64 tokens") {
    val tokens = Seq("a.b", "a.b.c", "1.2.3", "abcde.azer.azer", "aze$.azer.azer")

    tokens.foreach { token =>
      intercept[IllegalArgumentException] {
        Jwt.validate(token, secretKey, JwtAlgorithm.allHmac())
      }
      assert(!Jwt.isValid(token, secretKey, JwtAlgorithm.allHmac()), token)
    }
  }

  test("should invalidate expired tokens") {
    data foreach { d =>
      intercept[JwtExpirationException] {
        afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac())
      }
      assert(
        !afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac()),
        d.algo.fullName
      )
      intercept[JwtExpirationException] {
        afterExpirationJwt.validate(d.token, secretKeyOf(d.algo))
      }
      assert(!afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo)), d.algo.fullName)
    }

    dataRSA foreach { d =>
      intercept[JwtExpirationException] {
        afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA())
      }
      assert(
        !afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA()),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      intercept[JwtExpirationException] {
        afterExpirationJwt.validate(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA())
      }
      assert(
        !afterExpirationJwt.isValid(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA()),
        d.algo.fullName
      )
    }
  }

  test("should validate expired tokens with leeway") {
    val options = JwtOptions(leeway = 60)

    data foreach { d =>
      afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac(), options)
      assert(
        afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac(), options),
        d.algo.fullName
      )
      afterExpirationJwt.validate(d.token, secretKeyOf(d.algo), options)
      assert(afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo), options), d.algo.fullName)
    }

    dataRSA foreach { d =>
      afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
      assert(
        afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      afterExpirationJwt.validate(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options)
      assert(
        afterExpirationJwt.isValid(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options),
        d.algo.fullName
      )
    }
  }

  test("should invalidate early tokens") {
    data foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

      intercept[JwtNotBeforeException] {
        beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac())
      }
      assert(!beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac()), d.algo.fullName)
      intercept[JwtNotBeforeException] { beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo)) }
      assert(!beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo)), d.algo.fullName)
    }

    dataRSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

      intercept[JwtNotBeforeException] {
        beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA())
      }
      assert(
        !beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA()),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyEd25519, d.algo)

      intercept[JwtNotBeforeException] {
        beforeNotBeforeJwt.validate(token, publicKeyEd25519, JwtAlgorithm.allEdDSA())
      }
      assert(
        !beforeNotBeforeJwt.isValid(token, publicKeyEd25519, JwtAlgorithm.allEdDSA()),
        d.algo.fullName
      )
    }
  }

  test("should validate early tokens with leeway") {
    val options = JwtOptions(leeway = 60)

    data foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

      beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac(), options)
      assert(
        beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac(), options),
        d.algo.fullName
      )
      beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo), options)
      assert(beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo), options), d.algo.fullName)
    }

    dataRSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

      intercept[JwtNotBeforeException] {
        beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA())
      }
      assert(
        !beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA()),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyEd25519, d.algo)

      intercept[JwtNotBeforeException] {
        beforeNotBeforeJwt.validate(token, publicKeyEd25519, JwtAlgorithm.allEdDSA())
      }
      assert(
        !beforeNotBeforeJwt.isValid(token, publicKeyEd25519, JwtAlgorithm.allEdDSA()),
        d.algo.fullName
      )
    }
  }

  test("should invalidate wrong keys") {
    data foreach { d =>
      intercept[JwtValidationException] {
        validTimeJwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac())
      }
      assert(!validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac()), d.algo.fullName)
    }

    dataRSA foreach { d =>
      assert(!validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA()), d.algo.fullName)
    }

    dataEdDSA foreach { d =>
      assert(!validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allEdDSA()), d.algo.fullName)
    }
  }

  test("should fail on non-exposed algorithms") {
    data foreach { d =>
      intercept[JwtValidationException] {
        validTimeJwt.validate(d.token, secretKey, Seq.empty[JwtHmacAlgorithm])
      }
      assert(
        !validTimeJwt.isValid(d.token, secretKey, Seq.empty[JwtHmacAlgorithm]),
        d.algo.fullName
      )
    }

    data foreach { d =>
      intercept[JwtValidationException] {
        validTimeJwt.validate(d.token, secretKey, JwtAlgorithm.allRSA())
      }
      assert(!validTimeJwt.isValid(d.token, secretKey, JwtAlgorithm.allRSA()), d.algo.fullName)
    }

    dataRSA foreach { d =>
      intercept[JwtValidationException] {
        validTimeJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allHmac())
      }
      assert(!validTimeJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allHmac()), d.algo.fullName)
    }
  }

  test("should invalidate wrong algos") {
    val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
    assert(Jwt.decode(token).isFailure)
    intercept[JwtNonSupportedAlgorithm] { Jwt.decode(token).get }
  }

  test("should decode tokens with unknown algos depending on options") {
    val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJXVEYifQ.e30"
    val decoded = Jwt.decode(token, options = JwtOptions(signature = false))
    assert(decoded.isSuccess)
  }

  test("should skip expiration validation depending on options") {
    val options = JwtOptions(expiration = false)

    data foreach { d =>
      afterExpirationJwt.validate(d.token, secretKey, JwtAlgorithm.allHmac(), options)
      assert(
        afterExpirationJwt.isValid(d.token, secretKey, JwtAlgorithm.allHmac(), options),
        d.algo.fullName
      )
      afterExpirationJwt.validate(d.token, secretKeyOf(d.algo), options)
      assert(afterExpirationJwt.isValid(d.token, secretKeyOf(d.algo), options), d.algo.fullName)
    }

    dataRSA foreach { d =>
      afterExpirationJwt.validate(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
      assert(
        afterExpirationJwt.isValid(d.token, publicKeyRSA, JwtAlgorithm.allRSA(), options),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      afterExpirationJwt.validate(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options)
      assert(
        afterExpirationJwt.isValid(d.token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options),
        d.algo.fullName
      )
    }
  }

  test("should skip notBefore validation depending on options") {
    val options = JwtOptions(notBefore = false)

    data foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, secretKey, d.algo)

      beforeNotBeforeJwt.validate(token, secretKey, JwtAlgorithm.allHmac(), options)
      assert(
        beforeNotBeforeJwt.isValid(token, secretKey, JwtAlgorithm.allHmac(), options),
        d.algo.fullName
      )
      beforeNotBeforeJwt.validate(token, secretKeyOf(d.algo), options)
      assert(beforeNotBeforeJwt.isValid(token, secretKeyOf(d.algo), options), d.algo.fullName)
    }

    dataRSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyRSA, d.algo)

      beforeNotBeforeJwt.validate(token, publicKeyRSA, JwtAlgorithm.allRSA(), options)
      assert(
        beforeNotBeforeJwt.isValid(token, publicKeyRSA, JwtAlgorithm.allRSA(), options),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      val claimNotBefore = claimClass.startsAt(notBefore)
      val token = beforeNotBeforeJwt.encode(claimNotBefore, privateKeyEd25519, d.algo)

      beforeNotBeforeJwt.validate(token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options)
      assert(
        beforeNotBeforeJwt.isValid(token, publicKeyEd25519, JwtAlgorithm.allEdDSA(), options),
        d.algo.fullName
      )
    }
  }

  test("should skip signature validation depending on options") {
    val options = JwtOptions(signature = false)

    data foreach { d =>
      validTimeJwt.validate(d.token, "wrong key", JwtAlgorithm.allHmac(), options)
      assert(
        validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allHmac(), options),
        d.algo.fullName
      )
    }

    dataRSA foreach { d =>
      assert(
        validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allRSA(), options),
        d.algo.fullName
      )
    }

    dataEdDSA foreach { d =>
      assert(
        validTimeJwt.isValid(d.token, "wrong key", JwtAlgorithm.allEdDSA(), options),
        d.algo.fullName
      )
    }
  }

  def testTryAll(
      t: Try[(JwtHeader, JwtClaim, String)],
      exp: (JwtHeader, JwtClaim, String),
      clue: String
  ) = {
    assert(t.isSuccess, clue)
    val (h1, c1, s1) = t.get
    val (h2, c2, s2) = exp
    assertEquals(h1, h2)
    assertEquals(c1, c2)
    assertEquals(s1, s2)
  }

  def testTryAllWithoutSignature(
      t: Try[(JwtHeader, JwtClaim, String)],
      exp: (JwtHeader, JwtClaim, String),
      clue: String
  ) = {
    assert(t.isSuccess, clue)
    val (h1, c1, _) = t.get
    val (h2, c2, _) = exp
    assertEquals(h1, h2)
    assertEquals(c1, c2)
  }

  def testTryAllWithoutSignature(
      t: Try[(JwtHeader, JwtClaim, String)],
      exp: (JwtHeader, JwtClaim),
      clue: String
  ) = {
    assert(t.isSuccess, clue)
    val (h1, c1, _) = t.get
    val (h2, c2) = exp
    assertEquals(h1, h2)
    assertEquals(c1, c2)
  }

}
