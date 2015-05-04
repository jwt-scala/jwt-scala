package pdi.jwt

import scala.util.Try

import play.api.libs.json._

object JwtJson extends JwtCore[JwtHeader, JwtClaim] {
  def encode(header: JsObject, claim: JsObject, key: Option[String]): String =
    encode(Json.stringify(header), Json.stringify(claim), key, (header \ "alg" match {
      case JsString(algo) => Option(algo)
      case _ => None
    }))

  def encode(header: JsObject, claim: JsObject, key: String): String =
    encode(header, claim, Option(key))

  def encode(header: JsObject, claim: JsObject): String =
    encode(header, claim, None)

  def decodeAllJson(token: String, maybeKey: Option[String] = None): Try[(JsObject, JsObject, Option[String])] =
    decodeRawAllValidated(token, maybeKey).map { tuple =>
      val jsHeader = Json.parse(tuple._1).as[JsObject]
      val jsClaim = Json.parse(tuple._2).as[JsObject]

      // JWT is using second based timestamp
      // but all our helpers are using millis
      val notBefore = jsClaim \ "nbf" match {
        case JsNumber(nbf) => Option(nbf.toLong * 1000)
        case _ => None
      }

      val expiration = jsClaim \ "exp" match {
        case JsNumber(exp) => Option(exp.toLong * 1000)
        case _ => None
      }

      JwtTime.validateNowIsBetween(notBefore, expiration)
      (jsHeader, jsClaim, tuple._3)
    }

  def decodeJson(token: String, maybeKey: Option[String] = None): Try[JsObject] =
    decodeAllJson(token, maybeKey).map(_._2)

  def decodeJson(token: String, key: String): Try[JsObject] =
    decodeJson(token, Option(key))

  def decodeAll(token: String, maybeKey: Option[String] = None): Try[(JwtHeader, JwtClaim, Option[String])] =
    decodeAllJson(token, maybeKey).map { tuple =>
      (jwtHeaderReader.reads(tuple._1).get, jwtClaimReader.reads(tuple._2).get, tuple._3)
    }
}
