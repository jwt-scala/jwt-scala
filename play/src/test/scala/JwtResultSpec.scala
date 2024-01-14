package pdi.jwt

import akka.stream.Materializer
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.*
import play.api.test.*

class JwtResultSpec extends munit.FunSuite with PlayFixture {
  import pdi.jwt.JwtSession.*

  val app = new GuiceApplicationBuilder()
    .configure(Map("play.http.secret.key" -> secretKey))
    .build()

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  val HEADER_NAME = "Authorization"

  val session = JwtSession().withHeader(JwtHeader(JwtAlgorithm.HS256))

  test("JwtResult must support basic scenario") {
    implicit val request =
      FakeRequest().withHeaders(("Authorization", "Bearer " + session.serialize))
    var result: Result = Results.Ok

    // We can already get a JwtSession from our implicit RequestHeader
    assertEquals(result.jwtSession.claimData, Json.obj())

    // Setting a new empty JwtSession
    result = result.withNewJwtSession
    assertEquals(result.jwtSession.claimData, Json.obj())

    // Or from an existing JwtSession
    result = result.withJwtSession(session)
    assertEquals(result.jwtSession.claimData, Json.obj())

    // Or from a JsObject
    result = result.withJwtSession(Json.obj(("id", 1), ("key", "value")))
    assertEquals(result.jwtSession.claimData, Json.obj("id" -> 1, "key" -> "value"))

    // Or from (key, value)
    result = result.withJwtSession(("id", 1), ("key", "value"))
    assertEquals(result.jwtSession.claimData, Json.obj("id" -> 1, "key" -> "value"))

    // We can add stuff to the current session (only (String, String))
    result = result.addingToJwtSession(("key2", "value2"), ("key3", "value3"))
    assertEquals(
      result.jwtSession.claimData,
      Json.obj(
        "id" -> 1,
        "key" -> "value",
        "key2" -> "value2",
        "key3" -> "value3"
      )
    )

    // Or directly classes or objects if you have the correct implicit Writes
    result = result.addingToJwtSession("user", User(1, "Paul"))
    assertEquals(
      result.jwtSession.claimData,
      Json.obj(
        "id" -> 1,
        "key" -> "value",
        "key2" -> "value2",
        "key3" -> "value3",
        "user" -> Json.obj("id" -> 1, "name" -> "Paul")
      )
    )

    // Removing from session
    result = result.removingFromJwtSession("key2", "key3")
    assertEquals(
      result.jwtSession.claimData,
      Json.obj(
        "id" -> 1,
        "key" -> "value",
        "user" -> Json.obj("id" -> 1, "name" -> "Paul")
      )
    )

    // Refresh the current session
    result = result.refreshJwtSession
    assertEquals(
      result.jwtSession.claimData,
      Json.obj(
        "id" -> 1,
        "key" -> "value",
        "user" -> Json.obj("id" -> 1, "name" -> "Paul")
      )
    )

    // So, at the end, you can do
    assertEquals(result.jwtSession.getAs[User]("user"), Some(User(1, "Paul")))
  }
}
