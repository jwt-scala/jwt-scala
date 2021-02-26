package pdi.jwt

import java.time.Clock

import play.api.libs.json.{JsNumber, JsObject, JsString, Json, Writes}

class JwtJsonSpec extends JwtJsonCommonSpec[JsObject] with JsonFixture {

  import pdi.jwt.JwtJson._

  override def jwtJsonCommon(clock: Clock) = JwtJson(clock)

  describe("JwtJson") {
    it("should implicitly convert to JsValue") {
      assertResult(
        Json.obj(
          ("iss" -> "me"),
          ("aud" -> Some("you")),
          ("sub" -> "something"),
          ("exp" -> 15),
          ("nbf" -> 10),
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
          .toJsValue()
      }

      assertResult(
        Json.obj(
          ("typ" -> "JWT"),
          ("alg" -> "HS256")
        ),
        "Claim"
      ) {
        JwtHeader(JwtAlgorithm.HS256).toJsValue()
      }
    }

    it("should implicitly convert to JsValue when audience is many") {
      assertResult(
        Json.obj(
          ("iss" -> "me"),
          ("aud" -> Set("you", "another")),
          ("sub" -> "something"),
          ("exp" -> 15),
          ("nbf" -> 10),
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
          .toJsValue()
      }
    }

    it("should decode token with spaces") {
      val (_, claim, _) = defaultJwt.decodeJsonAll(tokenWithSpaces).get
      val expiration = BigDecimal("32086368000")
      assertResult(JsNumber(0)) {
        (claim \ "nbf").get
      }
      assertResult(JsNumber(expiration)) {
        (claim \ "exp").get
      }
      assertResult(JsString("bar")) {
        (claim \ "foo").get
      }
    }
  }

  case class ContentClass(userId: String)
  implicit val contentClassWrites: Writes[ContentClass] = Json.writes

  describe("JwtClaim") {
    it("should add content with Writes") {

      val content = ContentClass(userId = "testId")
      val claim = JwtClaim().expiresAt(10) + content

      assertResult(
        Json.obj(
          "exp" -> 10,
          "userId" -> "testId"
        )
      ) {
        claim.toJsValue()
      }
    }

    it("should set content with Writes") {

      val content = ContentClass(userId = "testId")
      val claim = JwtClaim().expiresAt(10).withContent(content)

      assertResult(
        Json.obj(
          "exp" -> 10,
          "userId" -> "testId"
        )
      ) {
        claim.toJsValue()
      }
    }
  }
}
