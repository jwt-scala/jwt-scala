package pdi.jwt

import java.time.Clock
import org.json4s._
import org.json4s.JsonDSL._

class JwtJson4sNativeSpec extends JwtJsonCommonSpec[JObject] with Json4sNativeFixture {
  import pdi.jwt.JwtJson4s._

  override def jwtJsonCommon(clock: Clock) = JwtJson4s(clock)

  describe("JwtJson") {
    it("should implicitly convert to JValue") {
      assertResult(
        (
          ("iss" -> "me") ~
            ("aud" -> Option("you")) ~
            ("sub" -> "something") ~
            ("exp" -> 15) ~
            ("nbf" -> 10) ~
            ("iat" -> 10)
        ),
        "Claim"
      ) {
        JwtClaim()
          .by("me")
          .to("you")
          .about("something")
          .issuedAt(10)
          .startsAt(10)
          .expiresAt(15)
          .toJValue()
      }

      assertResult(
        (
          ("typ" -> "JWT") ~
            ("alg" -> "HS256")
        ),
        "Claim"
      ) {
        JwtHeader(JwtAlgorithm.HS256).toJValue()
      }
    }

    it("should implicitly convert to JValue when audience is many") {
      assertResult(
        (
          ("iss" -> "me") ~
            ("aud" -> Set("you", "another")) ~
            ("sub" -> "something") ~
            ("exp" -> 15) ~
            ("nbf" -> 10) ~
            ("iat" -> 10)
        ),
        "Claim"
      ) {
        JwtClaim(audience = Some(Set("you", "another")))
          .by("me")
          .about("something")
          .issuedAt(10)
          .startsAt(10)
          .expiresAt(15)
          .toJValue()
      }
    }
  }
}
