package pdi.jwt

import java.time.Clock
import scala.annotation.nowarn

abstract class JwtJsonCommonSpec[J] extends munit.FunSuite with JsonCommonFixture[J] {
  import JwtJsonCommonSpec.JwtJsonUnderTest

  protected def jwtJsonCommon(clock: Clock): JwtJsonUnderTest[J]

  protected def testEncoding: Boolean = true

  lazy val defaultJwt: JwtJsonUnderTest[J] = jwtJsonCommon(Clock.systemUTC)
  lazy val afterExpirationJwt: JwtJsonUnderTest[J] = jwtJsonCommon(afterExpirationClock)
  lazy val validTimeJwt: JwtJsonUnderTest[J] = jwtJsonCommon(validTimeClock)

  def battleTestEncode(d: JsonDataEntryTrait[J], @nowarn key: String, jwt: JwtJsonUnderTest[J]) = {
    assertEquals(
      d.tokenEmpty,
      jwt.encode(claimJson),
      s"${d.algo.fullName} No header, no key, no algo"
    )
  }

  if (testEncoding) {
    test("should encode with no algorithm") {
      assertEquals(tokenEmpty, { defaultJwt.encode(headerEmptyJson, claimJson) }, "Unsigned key")
    }

    test("should encode HMAC") {
      dataJson.foreach { d => battleTestEncode(d, secretKey, defaultJwt) }
    }

    test("should encode RSA") {
      dataRSAJson.foreach { d => battleTestEncode(d, privateKeyRSA, defaultJwt) }
    }
  }
}

object JwtJsonCommonSpec {
  type JwtJsonUnderTest[J] = JwtJsonCommon[J, JwtHeader, JwtClaim]
}
