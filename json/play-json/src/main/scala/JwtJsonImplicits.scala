package pdi.jwt

import pdi.jwt.exceptions.{
  JwtNonNumberException,
  JwtNonStringException,
  JwtNonStringSetOrStringException,
  JwtNonSupportedAlgorithm
}
import play.api.libs.json._

trait JwtJsonImplicits {
  private def extractString(json: JsObject, fieldName: String): Option[String] =
    (json \ fieldName).toOption.flatMap {
      case JsString(value) => Option(value)
      case JsNull          => None
      case _               => throw new JwtNonStringException(fieldName)
    }

  private def extractStringSetOrString(json: JsObject, fieldName: String): Option[Set[String]] =
    (json \ fieldName).validateOpt[Set[String]] match {
      case JsSuccess(set, _) => set
      case JsError(_) =>
        (json \ fieldName).validateOpt[String] match {
          case JsSuccess(string, _) => string.map(s => Set(s))
          case JsError(_)           => throw new JwtNonStringSetOrStringException(fieldName)
        }
    }

  private def extractLong(json: JsObject, fieldName: String): Option[Long] =
    (json \ fieldName).toOption.flatMap {
      case JsNumber(value) => Option(value.toLong)
      case JsNull          => None
      case _               => throw new JwtNonNumberException(fieldName)
    }

  private def keyToPath(key: String): JsPath = new JsPath(List(new KeyPathNode(key)))

  implicit val jwtPlayJsonClaimReader: Reads[JwtClaim] = Reads { (json: JsValue) =>
    json match {
      case value: JsObject =>
        try {
          JsSuccess(
            JwtClaim.apply(
              issuer = extractString(value, "iss"),
              subject = extractString(value, "sub"),
              audience = extractStringSetOrString(value, "aud"),
              expiration = extractLong(value, "exp"),
              notBefore = extractLong(value, "nbf"),
              issuedAt = extractLong(value, "iat"),
              jwtId = extractString(value, "jti"),
              content =
                Json.stringify(value - "iss" - "sub" - "aud" - "exp" - "nbf" - "iat" - "jti")
            )
          )
        } catch {
          case e: JwtNonStringException => JsError(keyToPath(e.getKey), "error.expected.string")
          case e: JwtNonNumberException => JsError(keyToPath(e.getKey), "error.expected.number")
          case e: JwtNonStringSetOrStringException =>
            JsError(keyToPath(e.getKey), "error.expected.array")
        }
      case _ => JsError("error.expected.jsobject")
    }
  }

  implicit val jwtPlayJsonClaimWriter: Writes[JwtClaim] = Writes { (claim: JwtClaim) =>
    Json.parse(claim.toJson)
  }

  implicit val jwtPlayJsonHeaderReader: Reads[JwtHeader] = Reads { (json: JsValue) =>
    json match {
      case value: JsObject =>
        try {
          JsSuccess(
            JwtHeader.apply(
              algorithm = extractString(value, "alg").flatMap(JwtAlgorithm.optionFromString),
              typ = extractString(value, "typ"),
              contentType = extractString(value, "cty"),
              keyId = extractString(value, "kid")
            )
          )
        } catch {
          case e: JwtNonStringException    => JsError(keyToPath(e.getKey), "error.expected.string")
          case _: JwtNonSupportedAlgorithm => JsError(keyToPath("alg"), "error.expected.algorithm")
        }
      case _ => JsError("error.expected.jsobject")
    }
  }

  implicit val jwtPlayJsonHeaderWriter: Writes[JwtHeader] = Writes { (header: JwtHeader) =>
    Json.parse(header.toJson)
  }

  implicit class RichJwtClaim(claim: JwtClaim) {
    def toJsValue(): JsValue = jwtPlayJsonClaimWriter.writes(claim)

    def +[A](a: A)(implicit writes: Writes[A]): JwtClaim = {
      val s = Json.stringify(writes.writes(a))
      claim + s
    }

    def withContent[A](a: A)(implicit writes: Writes[A]): JwtClaim = {
      val s = Json.stringify(writes.writes(a))
      claim.withContent(s)
    }
  }

  implicit class RichJwtHeader(header: JwtHeader) {
    def toJsValue(): JsValue = jwtPlayJsonHeaderWriter.writes(header)
  }
}
