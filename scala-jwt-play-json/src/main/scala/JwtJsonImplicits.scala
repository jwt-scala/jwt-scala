package pdi.scala.jwt

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait JwtJsonImplicits {
  private def extractString(json: JsObject, fieldName: String): Option[String] = (json \ fieldName) match {
    case JsString(value) => Option(value)
    case _ => None
  }

  private def extractLong(json: JsObject, fieldName: String): Option[Long] = (json \ fieldName) match {
    case JsNumber(value) => Option(value.toLong)
    case _ => None
  }

  implicit val jwtClaimReader = new Reads[JwtClaim] {
    def reads(json: JsValue) = json match {
      case value: JsObject => JsSuccess(JwtClaim.apply(
        issuer = extractString(value, "iss"),
        subject = extractString(value, "sub"),
        audience = extractString(value, "aud"),
        expiration = extractLong(value, "exp"),
        notBefore = extractLong(value, "nbf"),
        issuedAt = extractLong(value, "iat"),
        jwtId = extractString(value, "jti"),
        content = Json.stringify(value - "iss" - "sub" - "aud" - "exp" - "nbf" - "iat" - "jti")
      ))
      case _ => JsError("error.expected.jsobject")
    }
  }

  implicit val jwtClaimWriter = new Writes[JwtClaim] {
    def writes(claim: JwtClaim) = Json.parse(claim.toJson)
  }

  implicit val jwtHeaderReader = Json.reads[JwtHeader]
  implicit val jwtHeaderWriter = Json.writes[JwtHeader]
}
