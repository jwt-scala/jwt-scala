package pdi.jwt
import java.time.{Clock, Duration}

import akka.stream.Materializer
import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._

class JwtSessionAsymetricSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with BeforeAndAfter
    with PlayFixture {

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  // Just for test, users shouldn't change the header name normally
  def HEADER_NAME = "Auth"
  def sessionTimeout = defaultMaxAge

  val privateKey: String =
    "MIIBOAIBAAJAZbLOel7f+8jUyAxPcrwbWZjxOoEPCXOIQfrv5VeVCxGJMYnsZeCjfN9JEGyMoVXY65nD5crMOGj6oF8V5APL4wIDAQABAkAtiJx4H8iLdEUI+LINvflE6XyAZE52PdsxJ4iHl+oslPG1cHNSiE46Ol1uSWvv6GD3VPjcfi+wTPe6nWQmnZ6ZAiEAqji6dSXzWTYHwHjrBZyUwqD8LXa5mrkGAht1SZhyBK0CIQCY8lFtMM1Td7O9hplwZLGVvXAbDKNaAmvcVZUWD1aUzwIgYKLwCA3Rh3YLFJQRKRBpy8zFHbJnUJV1+cBI580p/ckCICiI6C2xJmm9qsRLHPVdqncOCt0QX2amh6GQiP+ctwyfAiAVMw0Pa3lad/Q2Wt9M7I3FdoGiGlcfMEk1NzvPvtGTjA=="
  val publicKey: String =
    "MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAZbLOel7f+8jUyAxPcrwbWZjxOoEPCXOIQfrv5VeVCxGJMYnsZeCjfN9JEGyMoVXY65nD5crMOGj6oF8V5APL4wIDAQAB"

  // {"typ":"JWT","alg":"RS256"}
  val header = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9"
  val signature =
    "WrNldzMwkaki0eK_-S5VyZqJpCmkDtYoehh-R_Dvr6vNvUdZsJtugZReQ74zhm1ntWGIw5EP8G1vrzJ9sirLIQ"

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        Map(
          "play.http.session.privateKey" -> privateKey,
          "play.http.session.publicKey" -> publicKey,
          "play.http.session.jwtName" -> HEADER_NAME,
          "play.http.session.maxAge" -> sessionTimeout * 1000,
          "play.http.session.algorithm" -> "RS256",
          "play.http.session.tokenPrefix" -> ""
        )
      )
      .build()

  def session = JwtSession()
  def sessionCustom = JwtSession(JwtHeader(JwtAlgorithm.RS256), claimClass, signature)
  def tokenCustom = header + "." + playClaim64 + "." + signature

  "Init FakeApplication" must {
    "have the correct config" in {
      app.configuration.getOptional[String]("play.http.session.privateKey") mustEqual Option(
        privateKey
      )
      app.configuration.getOptional[String]("play.http.session.publicKey") mustEqual Option(
        publicKey
      )
      app.configuration.getOptional[String]("play.http.session.jwtName") mustEqual Option(
        HEADER_NAME
      )
      app.configuration.getOptional[String]("play.http.session.algorithm") mustEqual Option("RS256")
      app.configuration.getOptional[String]("play.http.session.tokenPrefix") mustEqual Option("")
      app.configuration.getOptional[Int]("play.http.session.maxAge") mustEqual Option(
        sessionTimeout * 1000
      )
    }
  }

  "JwtSession" must {
    "read default configuration" in {
      assert(JwtSession.defaultHeader == JwtHeader(JwtAlgorithm.RS256))
      assert(JwtSession.ALGORITHM == JwtAlgorithm.RS256)
    }

    "init" in {
      assert(session.headerData == Json.obj("typ" -> "JWT", "alg" -> "RS256"))
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
    header + ".eyJleHAiOjEzMDA4MTkzODAsInVzZXIiOnsiaWQiOjEsIm5hbWUiOiJQYXVsIn19.ZSTobcEIDXJfxhsIhHoe-ySBxHHKsHoYqVW5n0WHTtq3yN3X1eejfvONcaDi8Wq-EK9CM9VMeP1CIPmBX91M8g"
  )
  val sessionHeaderExp = Some(
    header + ".eyJleHAiOjEzMDA4MTk0MTF9.Vyr9qnNVAGNAqU1N_JkaiYUhVq3dgBrsjlW4gr4pdO9nIh1QeWABFi3ADKSC5Z7zubvH_WAx3X5A9SaKxp4_bg"
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
