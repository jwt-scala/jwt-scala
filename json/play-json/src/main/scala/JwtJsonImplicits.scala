package pdi.jwt

import play.api.libs.json._
import play.api.libs.functional.syntax._

import pdi.jwt.exceptions.{JwtNonStringException, JwtNonNumberException, JwtNonSupportedAlgorithm}

trait JwtJsonImplicits {
  private def extractString(json: JsObject, fieldName: String): Option[String] = (json \ fieldName).toOption.flatMap {
    case JsString(value) => Option(value)
    case JsNull => None
    case _ => throw new JwtNonStringException(fieldName)
  }

  private def extractLong(json: JsObject, fieldName: String): Option[Long] = (json \ fieldName).toOption.flatMap {
    case JsNumber(value) => Option(value.toLong)
    case JsNull => None
    case _ => throw new JwtNonNumberException(fieldName)
  }

  private def keyToPath(key: String): JsPath = new JsPath(List(new KeyPathNode(key)))

  implicit val jwtClaimReader = new Reads[JwtClaim] {
    def reads(json: JsValue) = json match {
      case value: JsObject =>
        try {
          JsSuccess(JwtClaim.apply(
            issuer = extractString(value, "iss"),
            subject = extractString(value, "sub"),
            audience = extractString(value, "aud"),
            expiration = extractLong(value, "exp"),
            notBefore = extractLong(value, "nbf"),
            issuedAt = extractLong(value, "iat"),
            jwtId = extractString(value, "jti"),
            content = Json.stringify(value - "iss" - "sub" - "aud" - "exp" - "nbf" - "iat" - "jti")
          ))
        } catch {
          case e : JwtNonStringException => JsError(keyToPath(e.getKey), "error.expected.string")
          case e : JwtNonNumberException => JsError(keyToPath(e.getKey), "error.expected.number")
        }
      case _ => JsError("error.expected.jsobject")
    }
  }

  implicit val jwtClaimWriter = new Writes[JwtClaim] {
    def writes(claim: JwtClaim) = Json.parse(claim.toJson)
  }

  implicit val jwtHeaderReader = new Reads[JwtHeader] {
    def reads(json: JsValue) = json match {
      case value: JsObject =>
        try {
          JsSuccess(JwtHeader.apply(
            algorithm = extractString(value, "alg").map(JwtAlgorithm.fromString),
            typ = extractString(value, "typ"),
            contentType = extractString(value, "cty")
          ))
        } catch {
          case e : JwtNonStringException => JsError(keyToPath(e.getKey), "error.expected.string")
          case e : JwtNonSupportedAlgorithm => JsError(keyToPath("alg"), "error.expected.algorithm")
        }
      case _ => JsError("error.expected.jsobject")
    }
  }

  implicit val jwtHeaderWriter = new Writes[JwtHeader] {
    def writes(header: JwtHeader) = Json.parse(header.toJson)
  }
}
