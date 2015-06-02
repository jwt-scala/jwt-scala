package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import pdi.jwt._

import models.User

class Application extends Controller with Secured {
  val passwords = Seq("red", "blue", "green")

  def index = Action {
    Ok(views.html.index())
  }

  private val loginForm: Reads[(String, String)] =
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read[String] tupled

  def login = Action(parse.json) { implicit request =>
    request.body.validate(loginForm).fold(
      errors => {
        BadRequest(JsError.toJson(errors))
      },
      form => {
        if (passwords.contains(form._2)) {
          Ok.addingToJwtSession("user", User(form._1))
        } else {
          Unauthorized
        }
      }
    )
  }

  def publicApi = Action {
    Ok("That was easy!")
  }

  def privateApi = Authenticated {
    Ok("Only the best can see that.")
  }

  def adminApi = Admin {
    Ok("Top secret data. Hopefully, nobody will ever access it.")
  }

}
