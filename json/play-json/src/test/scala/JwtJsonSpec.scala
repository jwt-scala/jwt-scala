package pdi.jwt

import play.api.libs.json.{Json, JsObject}

class JwtJsonSpec extends JwtJsonCommonSpec[JsObject] with JsonFixture {
  val jwtJsonCommon = JwtJson

  describe("JwtJson") {
    it("should implicitly convert to JsValue") {
      assertResult(Json.obj(
        ("iss" -> "me"),
        ("aud" -> "you"),
        ("sub" -> "something"),
        ("exp" -> 15),
        ("nbf" -> 10),
        ("iat" -> 10)
      ), "Claim") {
        JwtClaim().by("me").to("you").about("something").issuedAt(10).startsAt(10).expiresAt(15).toJsValue
      }

      assertResult(Json.obj(
        ("typ" -> "JWT"),
        ("alg" -> "HS256")
      ), "Claim") {
        JwtHeader(JwtAlgorithm.HS256).toJsValue
      }
    }
  }
}
