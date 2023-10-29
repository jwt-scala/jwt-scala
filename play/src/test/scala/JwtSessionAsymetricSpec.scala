package pdi.jwt
import java.time.{Clock, Duration}

import akka.stream.Materializer
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.mvc.*
import play.api.test.Helpers.*

class JwtSessionAsymetricSpec extends munit.FunSuite with PlayFixture {

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

  val app =
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

  implicit lazy val conf: Configuration = app.configuration
  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val Action: DefaultActionBuilder =
    app.injector.instanceOf(classOf[DefaultActionBuilder])

  val session = JwtSession()
  val sessionCustom = JwtSession(JwtHeader(JwtAlgorithm.RS256), claimClass, signature)
  val tokenCustom = s"$header.$playClaim64.$signature"
  // Order in the Json changed for Scala 2.13 so this is correct too
  val tokenCustom2 =
    s"$header.eyJodHRwOi8vZXhhbXBsZS5jb20vaXNfcm9vdCI6dHJ1ZSwiaXNzIjoiam9lIiwiZXhwIjoxMzAwODE5MzgwfQ.XCvpOGm7aPRy5hozuniyxFJJOMSdo5VYykpZmiGJ3d37WZAIHCrUI1TtkEIU3IbOny2fevilILBliPNgrXl3tA"
  // Order changed again for Scala 3 (!!!)
  val tokenCustom3 =
    s"$header.eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.NCDbnwlfFuF28QZte2WU6tSl_H9q7O9ujOS8c9FcPvL2kEeb2q9TFcW-v5X8Si5YzRdY78y7pBtsF3pyr15ROA"

  test("Init FakeApplication with the correct config") {
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.privateKey"),
      Option(privateKey)
    )
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.publicKey"),
      Option(
        publicKey
      )
    )
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.jwtName"),
      Option(
        HEADER_NAME
      )
    )
    assertEquals(
      app.configuration.getOptional[String]("play.http.session.algorithm"),
      Option("RS256")
    )
    assertEquals(app.configuration.getOptional[String]("play.http.session.tokenPrefix"), Option(""))
    assertEquals(
      app.configuration.getOptional[Long]("play.http.session.maxAge"),
      Option(sessionTimeout * 1000)
    )
  }

  test("JwtSession must read default configuration") {
    assertEquals(JwtSession.defaultHeader, JwtHeader(JwtAlgorithm.RS256))
    assertEquals(JwtSession.ALGORITHM, JwtAlgorithm.RS256)
  }

  test("JwtSession must init") {
    assertEquals(session.headerData, Json.obj("typ" -> "JWT", "alg" -> "RS256"))
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
    header + ".eyJleHAiOjEzMDA4MTkzODAsInVzZXIiOnsiaWQiOjEsIm5hbWUiOiJQYXVsIn19.ZSTobcEIDXJfxhsIhHoe-ySBxHHKsHoYqVW5n0WHTtq3yN3X1eejfvONcaDi8Wq-EK9CM9VMeP1CIPmBX91M8g"
  )
  val sessionHeaderExp = Some(
    header + ".eyJleHAiOjEzMDA4MTk0MTF9.Vyr9qnNVAGNAqU1N_JkaiYUhVq3dgBrsjlW4gr4pdO9nIh1QeWABFi3ADKSC5Z7zubvH_WAx3X5A9SaKxp4_bg"
  )

  test("RichResult must access app with no user") {
    val result = get(classicAction)
    val result2 = get(securedAction)

    assertEquals(status(result), OK)
    assertEquals(status(result2), UNAUTHORIZED)
    assertEquals(jwtHeader(result), None)
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
