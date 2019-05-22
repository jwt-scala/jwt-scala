package pdi.jwt

import java.time.Clock
import play.api.libs.json.{Json, JsObject, JsNumber, JsString}

class JwtJsonSpec extends JwtJsonCommonSpec[JsObject] with JsonFixture {
  import pdi.jwt.JwtJson._

  override def jwtJsonCommon(clock: Clock) = JwtJson(clock)

  describe("JwtJson") {
    it("should implicitly convert to JsValue") {
      assertResult(Json.obj(
        ("iss" -> "me"),
        ("aud" -> Set("you")),
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

    it("should decode token with spaces") {
      val (header, claim, signature) = defaultJwt.decodeJsonAll(tokenWithSpaces).get
      val expiration = BigDecimal("32086368000")
      assertResult(JsNumber(0)) { (claim \ "nbf").get }
      assertResult(JsNumber(expiration)) { (claim \ "exp").get }
      assertResult(JsString("bar")) { (claim \ "foo").get }
    }
  }
}
