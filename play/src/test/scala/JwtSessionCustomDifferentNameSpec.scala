package pdi.jwt

import java.time.{Clock, Duration}

import org.apache.pekko.stream.Materializer
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.*
import play.api.test.*
import play.api.test.Helpers.*

class JwtSessionCustomDifferentNameSpec extends munit.FunSuite with Injecting with PlayFixture {

  // Just for test, users shouldn't change the header name normally
  def HEADER_NAME = "Auth"
  def RESPONSE_HEADER_NAME = "Set-Auth"
  def sessionTimeout = defaultMaxAge

  val header = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9"
  val signature =
    "3FQn0RsztnK6i8x8Vi8k6WEsvzfnKDF2yx9WPeeiC1gu6yWZAMmCvzZi05A3d9sx2GwFfkVFPXgk_erYoizFxw"

  val app =
    new GuiceApplicationBuilder()
      .configure(
        Map.apply[String, Any](
          "play.http.secret.key" -> secretKey,
          "play.http.session.jwtName" -> HEADER_NAME,
          "play.http.session.jwtResponseName" -> RESPONSE_HEADER_NAME,
          "play.http.session.maxAge" -> sessionTimeout * 1000,
          "play.http.session.algorithm" -> "HS512",
          "play.http.session.tokenPrefix" -> ""
        )
      )
      .build()

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  val session = JwtSession()
  val sessionCustom = JwtSession(JwtHeader(JwtAlgorithm.HS512), claimClass, signature)
  val tokenCustom = header + "." + playClaim64 + "." + signature
  // Order in the Json changed for Scala 2.13 so this is correct too
  val tokenCustom2 =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJodHRwOi8vZXhhbXBsZS5jb20vaXNfcm9vdCI6dHJ1ZSwiaXNzIjoiam9lIiwiZXhwIjoxMzAwODE5MzgwfQ.aK_C250FUCSYbfjAfUgvAHoOfqa3EAdadSYkO0xEt1LJijGR0b89t2bl9AZJXdM4azAFj4RbzPyxSpIVczlchA"
  val tokenCustom3 =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.ngZsdQj8p2wvUAo8xCbJPwganGPnG5UnLkg7VrE6NgmQdV16UITjlBajZxcai_U5PjQdeN-yJtyA5kxf8O5BOQ"

  test("Init FakeApplication with correct config") {
    assertEquals(app.configuration.getOptional[String]("play.http.secret.key"), Option(secretKey))
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.jwtName"),
      Option(HEADER_NAME)
    )
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.jwtResponseName"),
      Option(
        RESPONSE_HEADER_NAME
      )
    )
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.algorithm"),
      Option("HS512")
    )
    assertEquals(app.configuration.getOptional[String]("play.http.session.tokenPrefix"), Option(""))
    assertEquals(
      app.configuration.getOptional[Long]("play.http.session.maxAge"),
      Option(
        sessionTimeout * 1000
      )
    )
  }

  test("JwtSession must read default configuration") {
    assertEquals(JwtSession.defaultHeader, JwtHeader(JwtAlgorithm.HS512))
    assertEquals(JwtSession.ALGORITHM, JwtAlgorithm.HS512)
  }

  test("JwtSession must init") {
    assertEquals(session.headerData, Json.obj("typ" -> "JWT", "alg" -> "HS512"))
    assertEquals(session.claimData, Json.obj("exp" -> (validTime + sessionTimeout)))
    assertEquals(session.signature, "")
    assert(!session.isEmpty()) // There is the expiration date in the claim
  }

  test("JwtSession must serialize") {
    assert(Set(tokenCustom, tokenCustom2, tokenCustom3).contains(clue(sessionCustom.serialize)))
  }

  test("JwtSession must deserialize") {
    assertEquals(JwtSession.deserialize(tokenCustom), sessionCustom)
  }

  val sessionHeaderUser = Some(
    header + ".eyJleHAiOjEzMDA4MTkzODAsInVzZXIiOnsiaWQiOjEsIm5hbWUiOiJQYXVsIn19.nfhPaLvlRjXlq3o-B1FvHk0rG_ZsqMdnr9cR3GCK23iGZ4an6uxOr_FJCXX5sgtnMIx1uqQ3utgW9jyBqqFuUw"
  )
  val sessionHeaderExp = Some(
    header + ".eyJleHAiOjEzMDA4MTk0MTF9.B27yGau7FJWE_2ir6B4dqQkXh3DhgryR29nyjA-TuWNfx3H7kcRbWf2XrpMN3cCpU04Oi1cV5I0w8DVyO-h6Ig"
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
  }

  test("RichResult must access app with user") {
    val result = get(classicAction, sessionHeaderUser)
    val result2 = get(securedAction, sessionHeaderUser)

    assertEquals(status(result), OK)
    assertEquals(status(result2), OK)
    assertEquals(jwtHeader(result), sessionHeaderUser)
    assertEquals(jwtHeader(result2), sessionHeaderUser)
  }

  test("RichResult must move to the future!") {
    this.clock = Clock.offset(this.clock, Duration.ofSeconds(sessionTimeout + 1))
  }

  test("RichResult must timeout session") {
    val result = get(classicAction, sessionHeaderUser)
    val result2 = get(securedAction, sessionHeaderUser)

    assertEquals(status(result), OK)
    assertEquals(status(result2), UNAUTHORIZED)
    assertEquals(jwtHeader(result), sessionHeaderExp)
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
