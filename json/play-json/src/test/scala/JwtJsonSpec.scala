package pdi.jwt

import java.time.Clock
import scala.util.Failure
import scala.util.Success

import pdi.jwt.exceptions.JwtNonNumberException
import pdi.jwt.exceptions.JwtNonStringException
import play.api.libs.json.{JsNumber, JsObject, JsString, Json, Writes}

class JwtJsonSpec extends JwtJsonCommonSpec[JsObject] with JsonFixture {

  import pdi.jwt.JwtJson.*

  override def jwtJsonCommon(clock: Clock): JwtJson = JwtJson(clock)

  test("JwtJson should implicitly convert to JsValue") {
    assertEquals(
      JwtClaim()
        .by("me")
        .to("you")
        .about("something")
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJsValue(),
      Json.obj(
        ("iss" -> "me"),
        ("aud" -> Some("you")),
        ("sub" -> "something"),
        ("exp" -> 15),
        ("nbf" -> 10),
        ("iat" -> 10)
      ),
      "Claim"
    )

    assertEquals(
      JwtHeader(JwtAlgorithm.HS256).toJsValue(),
      Json.obj(
        ("typ" -> "JWT"),
        ("alg" -> "HS256")
      ),
      "Claim"
    )
  }

  test("JwtJson should implicitly convert to JsValue when audience is many") {
    assertEquals(
      JwtClaim(audience = Some(Set("you", "another")))
        .by("me")
        .about("something")
        .issuedAt(10)
        .startsAt(10)
        .expiresAt(15)
        .toJsValue(),
      Json.obj(
        ("iss" -> "me"),
        ("aud" -> Set("you", "another")),
        ("sub" -> "something"),
        ("exp" -> 15),
        ("nbf" -> 10),
        ("iat" -> 10)
      ),
      "Claim"
    )
  }

  test("JwtJson should decode token with spaces") {
    val (_, claim, _) = defaultJwt.decodeJsonAll(tokenWithSpaces).get
    val expiration = BigDecimal("32086368000")
    assertEquals((claim \ "nbf").get, JsNumber(0))
    assertEquals((claim \ "exp").get, JsNumber(expiration))
    assertEquals((claim \ "foo").get, JsString("bar"))
  }

  test("JwtJson should fail on an invalid issuer") {
    val header = """{"alg": "none"}"""
    val claim = """{"iss": 42}"""
    val token = s"${JwtBase64.encodeString(header)}.${JwtBase64.encodeString(claim)}."
    defaultJwt.decodeJsonAll(token) match {
      case Failure(JwtNonStringException("/iss")) => ()
      case Failure(e)                             => fail(s"Expected JwtNonStringException, got $e")
      case Success(_) => fail(s"Expected JwtNonStringException, got success")
    }
  }

  test("JwtJson should fail on an invalid expiration") {
    val header = """{"alg": "none"}"""
    val claim = """{"iss": "me", "exp": "wrong"}"""
    val token = s"${JwtBase64.encodeString(header)}.${JwtBase64.encodeString(claim)}."
    defaultJwt.decodeJsonAll(token) match {
      case Failure(JwtNonNumberException("/exp")) => ()
      case Failure(e)                             => fail(s"Expected JwtNonNumberException, got $e")
      case Success(_) => fail(s"Expected JwtNonStringException, got success")
    }
  }

  case class ContentClass(userId: String)
  implicit val contentClassWrites: Writes[ContentClass] = Json.writes

  test("JwtClaim should add content with Writes") {

    val content = ContentClass(userId = "testId")
    val claim = JwtClaim().expiresAt(10) + content

    assertEquals(
      claim.toJsValue(),
      Json.obj(
        "exp" -> 10,
        "userId" -> "testId"
      )
    )
  }

  test("JwtClaim should set content with Writes") {

    val content = ContentClass(userId = "testId")
    val claim = JwtClaim().expiresAt(10).withContent(content)

    assertEquals(
      claim.toJsValue(),
      Json.obj(
        "exp" -> 10,
        "userId" -> "testId"
      )
    )
  }
}
