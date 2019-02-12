package pdi.jwt

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.util.Timeout
import akka.stream.Materializer
import play.api.Configuration
import play.api.mvc._
import play.api.mvc.Results._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

case class User(id: Long, name: String)

trait PlayFixture extends Fixture {
  import pdi.jwt.JwtSession._


  def HEADER_NAME: String

  implicit val userFormat = Json.format[User]

  val user = User(1, "Paul")
  val userJson = Json.obj("id" -> 1, "name" -> "Paul")

  def loginAction(implicit conf:Configuration, Action:DefaultActionBuilder): EssentialAction = Action { implicit request =>
    val username = (request.body.asJson.get \ "username").as[String]
    val password = (request.body.asJson.get \ "password").as[String]

    password match {
      case "p4ssw0rd" => Ok.addingToJwtSession("user", user)
      case _ => BadRequest
    }
  }

  def logoutAction(implicit conf:Configuration, Action:DefaultActionBuilder): EssentialAction = Action {
    Ok.withoutJwtSession
  }

  def classicAction(implicit conf:Configuration, Action:DefaultActionBuilder): EssentialAction = Action { implicit request =>
    Ok.refreshJwtSession
  }

  def securedAction(implicit conf:Configuration, Action:DefaultActionBuilder): EssentialAction = Action { implicit request =>
    request.jwtSession.getAs[User]("user") match {
      case Some(u) => Ok.refreshJwtSession
      case _ => Unauthorized.withoutJwtSession
    }
  }

  def get(action: EssentialAction, header: Option[String] = None)(implicit conf:Configuration, mat:Materializer, Action:DefaultActionBuilder) = {
    var request = header match {
      case Some(h) => FakeRequest(GET, "/something").withHeaders((JwtSession.REQUEST_HEADER_NAME, h))
      case _ => FakeRequest(GET, "/something")
    }

    call(action, request)
  }

  def post(action: EssentialAction, body: JsObject)(implicit mat:Materializer, Action:DefaultActionBuilder) = {
    call(action, FakeRequest(POST, "/something").withJsonBody(body))
  }

  def jwtHeader(of: Future[Result])(implicit timeout: Timeout, conf:Configuration): Option[String] = header(JwtSession.RESPONSE_HEADER_NAME, of)(timeout)
}
