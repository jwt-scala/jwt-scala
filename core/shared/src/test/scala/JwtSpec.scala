package pdi.jwt

import scala.util.Success
import scala.util.Try

import pdi.jwt.exceptions.*

class JwtSpec extends munit.FunSuite with Fixture with JwtPlatformSpec {
  val afterExpirationJwt: Jwt = Jwt(afterExpirationClock)
  val beforeNotBeforeJwt: Jwt = Jwt(beforeNotBeforeClock)
  val afterNotBeforeJwt: Jwt = Jwt(afterNotBeforeClock)
  val validTimeJwt: Jwt = Jwt(validTimeClock)

  test("should parse JSON with spaces") {
    assert(Jwt.isValid(tokenWithSpaces))
  }

  test("should decode subject with dashes") {
    Jwt.decode(validTimeJwt.encode(s"""{"sub":"das-hed"""")) match {
      case Success(jwt) => assertEquals(jwt.subject, Option("das-hed"))
      case _            => fail("failed decoding token")
    }
  }

  test("should encode Hmac") {
    data.foreach { d => battleTestEncode(d, secretKey, validTimeJwt) }
  }

  test("should encode RSA") {
    dataRSA.foreach { d => battleTestEncode(d, privateKeyRSA, validTimeJwt) }
  }

  test("should encode EdDSA") {
    dataEdDSA.foreach { d => battleTestEncode(d, privateKeyEd25519, validTimeJwt) }
  }

  def oneLine(key: String) = key.replaceAll("\r\n", " ").replaceAll("\n", " ")

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
