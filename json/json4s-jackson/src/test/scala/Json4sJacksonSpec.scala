package pdi.jwt

import java.time.Clock
import org.json4s._
import org.json4s.JsonDSL._

class JwtJson4sJacksonSpec extends JwtJsonCommonSpec[JObject] with Json4sJacksonFixture {
  import pdi.jwt.JwtJson4s._

  override def jwtJsonCommon(clock: Clock) = JwtJson4s(clock)

  test("JwtJson should implicitly convert to JValue") {
    assertEquals(
      JwtClaim()
        .by("me")
        .to("you")
        .about("something")
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJValue(),
      (
        ("iss" -> "me") ~
          ("aud" -> Option("you")) ~
          ("sub" -> "something") ~
          ("exp" -> 15) ~
          ("nbf" -> 10) ~
          ("iat" -> 10)
      ),
      "Claim"
    )

    assertEquals(
      JwtHeader(JwtAlgorithm.HS256).toJValue(),
      (("typ" -> "JWT") ~ ("alg" -> "HS256")),
      "Claim"
    )
  }

  test("JwtJson should implicitly convert to JValue when audience is many") {
    assertEquals(
      JwtClaim(audience = Some(Set("you", "another")))
        .by("me")
        .about("something")
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJValue(),
      (
        ("iss" -> "me") ~
          ("aud" -> Set("you", "another")) ~
          ("sub" -> "something") ~
          ("exp" -> 15) ~
          ("nbf" -> 10) ~
          ("iat" -> 10)
      ),
      "Claim"
    )
  }
}
