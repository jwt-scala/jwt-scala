package pdi.jwt

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

class JwtJson4sJacksonSpec extends JwtJsonCommonSpec[JObject] with Json4sJacksonFixture {
  val jwtJsonCommon = JwtJson4s

  describe("JwtJson") {
    it("should implicitly convert to JValue") {
      assertResult((
        ("iss" -> "me") ~
        ("aud" -> "you") ~
        ("sub" -> "something") ~
        ("exp" -> 15) ~
        ("nbf" -> 10) ~
        ("iat" -> 10)
      ), "Claim") {
        JwtClaim().by("me").to("you").about("something").issuedAt(10).startsAt(10).expiresAt(15).toJValue
      }

      assertResult((
        ("typ" -> "JWT") ~
        ("alg" -> "HS256")
      ), "Claim") {
        JwtHeader(JwtAlgorithm.HS256).toJValue
      }
    }
  }
}
