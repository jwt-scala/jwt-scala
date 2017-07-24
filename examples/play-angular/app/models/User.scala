package models

import play.api.libs.json._

case class User(name: String) {
  def isAdmin: Boolean = name.toLowerCase == "admin"
}

object User {
  implicit val userFormat = Json.format[User]
}
