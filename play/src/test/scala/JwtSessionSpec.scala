package pdi.jwt

import akka.stream.Materializer
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import scala.concurrent.duration.Duration

class JwtSessionSpec extends munit.FunSuite with PlayFixture {
  val app =
    new GuiceApplicationBuilder()
      .configure(
        Map(
          "play.http.secret.key" -> secretKey
        )
      )
      .build()

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  def HEADER_NAME = "Authorization"
  val session = JwtSession().withHeader(JwtHeader(JwtAlgorithm.HS256))
  val session2 = session ++ (("a", 1), ("b", "c"), ("e", true), ("f", Seq(1, 2, 3)), ("user", user))
  val session3 = JwtSession(
    JwtHeader(JwtAlgorithm.HS256),
    claimClass,
    "IPSERPZc5wyxrZ4Yiq7l31wFk_qaDY5YrnfLjIC0Lmc"
  )
  // This is session3 serialized (if no bug...)
  val token =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9." + claim64 + ".IPSERPZc5wyxrZ4Yiq7l31wFk_qaDY5YrnfLjIC0Lmc"
  // Order in the Json changed for Scala 2.13 so this is correct too
  val token2 =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJodHRwOi8vZXhhbXBsZS5jb20vaXNfcm9vdCI6dHJ1ZSwiaXNzIjoiam9lIiwiZXhwIjoxMzAwODE5MzgwfQ.XCvpOGm7aPRy5hozuniyxFJJOMSdo5VYykpZmiGJ3d37WZAIHCrUI1TtkEIU3IbOny2fevilILBliPNgrXl3tA"

  test("Init FakeApplication with the correct config") {
    assertEquals(app.configuration.getOptional[String]("play.http.secret.key"), Option(secretKey))
    assertEquals(app.configuration.getOptional[Duration]("play.http.session.maxAge"), None)
  }

  test("JwtSession must read default configuration") {
    assertEquals(JwtSession.defaultHeader, JwtHeader(JwtAlgorithm.HS256))
    assert(JwtSession.MAX_AGE.isEmpty)
  }

  test("JwtSession must init") {
    assertEquals(session.headerData, Json.obj("typ" -> "JWT", "alg" -> "HS256"))
    assertEquals(session.claimData, Json.obj())
    assertEquals(session.signature, "")
    assert(session.isEmpty())
  }

  test("JwtSession must add stuff") {
    assertEquals((session + Json.obj("a" -> 1)).claimData, Json.obj("a" -> 1))
    assertEquals((session + ("a", 1) + ("b", "c")).claimData, Json.obj("a" -> 1, "b" -> "c"))
    assertEquals((session + ("user", user)).claimData, Json.obj("user" -> userJson))
    assertEquals((session ++ (("a", 1), ("b", "c"))).claimData, Json.obj("a" -> 1, "b" -> "c"))

    assertEquals(
      (session + ("a", 1) + ("b", "c") + ("user", user)).claimData,
      Json.obj(
        "a" -> 1,
        "b" -> "c",
        "user" -> userJson
      )
    )

    val sessionBis = session + ("a", 1) + ("b", "c")
    val sessionTer = sessionBis ++ (("d", true), ("e", 42))
    val sessionQuad = sessionTer + ("user", user)
    assertEquals(
      sessionQuad.claimData,
      Json.obj(
        "a" -> 1,
        "b" -> "c",
        "d" -> true,
        "e" -> 42,
        "user" -> userJson
      )
    )
  }

  test("JwtSession must remove stuff") {
    assertEquals((session2 - "e" - "f" - "user").claimData, Json.obj("a" -> 1, "b" -> "c"))
    assertEquals((session2 -- ("e", "f", "user")).claimData, Json.obj("a" -> 1, "b" -> "c"))
  }

  test("JwtSession must get stuff") {
    assertEquals(session2("a"), Option(JsNumber(1)))
    assertEquals(session2("b"), Option(JsString("c")))
    assertEquals(session2("e"), Option(JsBoolean(true)))
    assertEquals(session2("f"), Option(Json.arr(1, 2, 3)))
    assert(session2("nope") match { case None => true; case _ => false })
    assertEquals(session2.get("a"), Option(JsNumber(1)))
    assertEquals(session2.get("b"), Option(JsString("c")))
    assertEquals(session2.get("e"), Option(JsBoolean(true)))
    assertEquals(session2.get("f"), Option(Json.arr(1, 2, 3)))
    assert(session2.get("nope") match { case None => true; case _ => false })
    assertEquals(session2.getAs[User]("user"), Option(user))
    assertEquals(session2.getAs[User]("nope"), None)
  }

  test("JwtSession must test emptiness") {
    assert(session.isEmpty())
    assert(!session2.isEmpty())
  }

  test("JwtSession must serialize") {
    assert(Set(token, token2).contains(session3.serialize))
  }

  test("JwtSession must deserialize") {
    assertEquals(JwtSession.deserialize(token)(conf, validTimeClock), session3)
  }

  val sessionHeader = Some(
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjp7ImlkIjoxLCJuYW1lIjoiUGF1bCJ9fQ.KBHKQarAQMse-4Conoi22XShk1ky--XXKAx4kMp6v-M"
  )

  test("RichResult must access app with no user") {
    val result = get(classicAction)
    val result2 = get(securedAction)

    assertEquals(status(result), OK)
    assertEquals(status(result2), UNAUTHORIZED)
    assertEquals(jwtHeader(result), None)
    assertEquals(jwtHeader(result2), None)
  }

  test("RichResult must fail to login") {
    val result = post(loginAction, Json.obj("username" -> "whatever", "password" -> "wrong"))
    assertEquals(status(result), BAD_REQUEST)
    assertEquals(jwtHeader(result), None)
  }

  test("RichResult must login") {
    val result = post(loginAction, Json.obj("username" -> "whatever", "password" -> "p4ssw0rd"))
    assertEquals(status(result), OK)
    assertEquals(jwtHeader(result), sessionHeader)
  }

  test("RichResult must access app with user") {
    val result = get(classicAction, sessionHeader)
    val result2 = get(securedAction, sessionHeader)

    assertEquals(status(result), OK)
    assertEquals(status(result2), OK)
    // Wuuut? Why None? Because since there is no "session.maxAge", we don't need to refresh the token
    // it's up to the client-side code to save it as long as it needs it
    assertEquals(jwtHeader(result), None)
    assertEquals(jwtHeader(result2), None)
  }

  test("RichResult must logout") {
    val result = get(logoutAction)
    assertEquals(status(result), OK)
    assertEquals(jwtHeader(result), None)
  }

  test("RichResult must access app with no user again") {
    val result = get(classicAction)
    val result2 = get(securedAction)

    assertEquals(status(result), OK)
    assertEquals(status(result2), UNAUTHORIZED)
    assertEquals(jwtHeader(result), None)
    assertEquals(jwtHeader(result2), None)
  }
}
