package pdi.jwt

import akka.stream.Materializer
import java.time.{Duration, Clock}
import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.api.test.Helpers._

class JwtSessionCustomDifferentNameSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with BeforeAndAfter
    with Injecting
    with PlayFixture {
  import pdi.jwt.JwtSession._

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  // Just for test, users shouldn't change the header name normally
  def HEADER_NAME = "Auth"
  def RESPONSE_HEADER_NAME = "Set-Auth"
  def sessionTimeout = defaultMaxAge

  val header = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9"
  val signature =
    "3FQn0RsztnK6i8x8Vi8k6WEsvzfnKDF2yx9WPeeiC1gu6yWZAMmCvzZi05A3d9sx2GwFfkVFPXgk_erYoizFxw"

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        Map(
          "play.http.secret.key" -> secretKey,
          "play.http.session.jwtName" -> HEADER_NAME,
          "play.http.session.jwtResponseName" -> RESPONSE_HEADER_NAME,
          "play.http.session.maxAge" -> sessionTimeout * 1000,
          "play.http.session.algorithm" -> "HS512",
          "play.http.session.tokenPrefix" -> ""
        )
      )
      .build()

  def session = JwtSession()
  def sessionCustom = JwtSession(JwtHeader(JwtAlgorithm.HS512), claimClass, signature)
  def tokenCustom = header + "." + playClaim64 + "." + signature

  "Init FakeApplication" must {
    "have the correct config" in {
      app.configuration.getOptional[String]("play.http.secret.key") mustEqual Option(secretKey)
      app.configuration.getOptional[String]("play.http.session.jwtName") mustEqual Option(
        HEADER_NAME
      )
      app.configuration.getOptional[String]("play.http.session.jwtResponseName") mustEqual Option(
        RESPONSE_HEADER_NAME
      )
      app.configuration.getOptional[String]("play.http.session.algorithm") mustEqual Option("HS512")
      app.configuration.getOptional[String]("play.http.session.tokenPrefix") mustEqual Option("")
      app.configuration.getOptional[Int]("play.http.session.maxAge") mustEqual Option(
        sessionTimeout * 1000
      )
    }
  }

  "JwtSession" must {
    "read default configuration" in {
      assert(JwtSession.defaultHeader == JwtHeader(JwtAlgorithm.HS512))
      assert(JwtSession.ALGORITHM == JwtAlgorithm.HS512)
    }

    "init" in {
      assert(session.headerData == Json.obj("typ" -> "JWT", "alg" -> "HS512"))
      assert(session.claimData == Json.obj("exp" -> (validTime + sessionTimeout)))
      assert(session.signature == "")
      assert(!session.isEmpty) // There is the expiration date in the claim
    }

    "serialize" in {
      assert(sessionCustom.serialize == tokenCustom)
    }

    "deserialize" in {
      assert(JwtSession.deserialize(tokenCustom) == sessionCustom)
    }
  }

  val sessionHeaderUser = Some(
    header + ".eyJleHAiOjEzMDA4MTkzODAsInVzZXIiOnsiaWQiOjEsIm5hbWUiOiJQYXVsIn19.nfhPaLvlRjXlq3o-B1FvHk0rG_ZsqMdnr9cR3GCK23iGZ4an6uxOr_FJCXX5sgtnMIx1uqQ3utgW9jyBqqFuUw"
  )
  val sessionHeaderExp = Some(
    header + ".eyJleHAiOjEzMDA4MTk0MTF9.B27yGau7FJWE_2ir6B4dqQkXh3DhgryR29nyjA-TuWNfx3H7kcRbWf2XrpMN3cCpU04Oi1cV5I0w8DVyO-h6Ig"
  )

  "RichResult" must {
    "access app with no user" in {
      val result = get(classicAction)
      val result2 = get(securedAction)

      status(result) mustEqual OK
      status(result2) mustEqual UNAUTHORIZED
      jwtHeader(result) mustEqual None
      jwtHeader(result2) mustEqual None
    }

    "fail to login" in {
      val result = post(loginAction, Json.obj("username" -> "whatever", "password" -> "wrong"))
      status(result) mustEqual BAD_REQUEST
      jwtHeader(result) mustEqual None
    }

    "login" in {
      val result = post(loginAction, Json.obj("username" -> "whatever", "password" -> "p4ssw0rd"))
      status(result) mustEqual OK
      jwtHeader(result) mustEqual sessionHeaderUser
    }

    "access app with user" in {
      val result = get(classicAction, sessionHeaderUser)
      val result2 = get(securedAction, sessionHeaderUser)

      status(result) mustEqual OK
      status(result2) mustEqual OK
      jwtHeader(result) mustEqual sessionHeaderUser
      jwtHeader(result2) mustEqual sessionHeaderUser
    }

    "move to the future!" in {
      this.clock = Clock.offset(this.clock, Duration.ofSeconds(sessionTimeout + 1))
    }

    "timeout session" in {
      val result = get(classicAction, sessionHeaderUser)
      val result2 = get(securedAction, sessionHeaderUser)

      status(result) mustEqual OK
      status(result2) mustEqual UNAUTHORIZED
      jwtHeader(result) mustEqual sessionHeaderExp
      jwtHeader(result2) mustEqual None
    }

    "logout" in {
      val result = get(logoutAction)
      status(result) mustEqual OK
      jwtHeader(result) mustEqual None
    }

    "access app with no user again" in {
      val result = get(classicAction)
      val result2 = get(securedAction)

      status(result) mustEqual OK
      status(result2) mustEqual UNAUTHORIZED
      jwtHeader(result) mustEqual None
      jwtHeader(result2) mustEqual None
    }
  }
}
