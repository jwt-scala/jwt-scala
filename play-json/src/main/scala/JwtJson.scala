package pdi.jwt

import scala.util.Try

import play.api.libs.json._

object JwtJson extends JwtCore[JwtHeader, JwtClaim] {
  protected def parseHeader(header: String): JwtHeader = jwtHeaderReader.reads(Json.parse(header)).get
  protected def parseClaim(claim: String): JwtClaim = jwtClaimReader.reads(Json.parse(claim)).get

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

  def encode(header: JsObject, claim: JsObject, key: Option[String]): String =
    encode(Json.stringify(header), Json.stringify(claim), key, (header \ "alg" match {
      case JsString(algo) => Option(JwtAlgorithm.fromString(algo))
      case JsNull => None
      case _ : JsUndefined => None
      case _ => throw new JwtNonStringException("alg")
    }))

  def encode(header: JsObject, claim: JsObject, key: String): String =
    encode(header, claim, Option(key))

  def encode(header: JsObject, claim: JsObject): String =
    encode(header, claim, None)

  def decodeAllJson(token: String, maybeKey: Option[String] = None): Try[(JsObject, JsObject, Option[String])] =
    decodeRawAll(token, maybeKey).map { tuple =>
      (Json.parse(tuple._1).as[JsObject], Json.parse(tuple._2).as[JsObject], tuple._3)
    }

  def decodeJson(token: String, maybeKey: Option[String] = None): Try[JsObject] =
    decodeAllJson(token, maybeKey).map(_._2)

  def decodeJson(token: String, key: String): Try[JsObject] =
    decodeJson(token, Option(key))
}
