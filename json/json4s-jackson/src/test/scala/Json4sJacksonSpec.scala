package pdi.jwt

import java.time.Clock

import org.json4s.*
import org.json4s.JsonDSL.*

class JwtJson4sJacksonSpec extends JwtJsonCommonSpec[JObject] with Json4sJacksonFixture {
  import pdi.jwt.JwtJson4s.*

  override def jwtJsonCommon(clock: Clock) = JwtJson4s(clock)

  test("JwtJson should implicitly convert to JValue") {
    assertEquals(
      JwtClaim()
        .by("me")
        .to("you")
        .about("something")
        .withScope(Set("email", "profile"))
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJValue(),
      (
        ("iss" -> "me") ~
          ("aud" -> Option("you")) ~
          ("sub" -> "something") ~
          ("scope" -> Option(Set("email", "profile"))) ~
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
        .withScope(Set("email", "profile"))
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJValue(),
      (
        ("iss" -> "me") ~
          ("aud" -> Set("you", "another")) ~
          ("sub" -> "something") ~
          ("scope" -> Set("email", "profile")) ~
          ("exp" -> 15) ~
          ("nbf" -> 10) ~
          ("iat" -> 10)
      ),
      "Claim"
    )
  }
}
