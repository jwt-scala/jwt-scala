package pdi.scala.jwt

import play.api.Play
/*import play.api.libs.json.{Json, JsValue, JsObject, Reads, Writes}*/
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.functional.syntax._

case class JwtSession(
  headerData: JsObject,
  claimData: JsObject,
  signature: Option[String]
) {
  def + (value: JsObject): JwtSession = this.copy(claimData = claimData.deepMerge(value))

  def + [T](key: String, value: JsValue): JwtSession = this + new JsObject(Seq(key -> value))

  def + [T](key: String, value: T)(implicit writer: Writes[T]): JwtSession = this + (key, writer.writes(value))

  def + (fields: (String, JsValueWrapper)*): JwtSession = this + Json.obj(fields: _*)

  def - (fieldNames: String*): JwtSession = this.copy(claimData = fieldNames.foldLeft(claimData) {
    (data, fieldName) => (data - fieldName)
  })

  def get(fieldName: String): JsValue = claimData \ fieldName

  def getAs[T](fieldName: String)(implicit reader: Reads[T]): Option[T] = reader.reads(get(fieldName)).asOpt

  def apply(fieldName: String): JsValue = get(fieldName)

  lazy val isEmpty: Boolean = claimData.keys.isEmpty

  def claim: JwtClaim = jwtClaimReader.reads(claimData).get
  def header: JwtHeader = jwtHeaderReader.reads(headerData).get

  def serialize: String = JwtJson.encode(headerData, claimData, JwtSession.key)

  def withClaim(claim: JwtClaim): JwtSession = this.copy(claimData = JwtSession.asJsObject(claim))
  def withHeader(header: JwtHeader): JwtSession = this.copy(headerData = JwtSession.asJsObject(header))

  def refresh: JwtSession = JwtSession.MAX_AGE.map(sec => this + Json.obj("exp" -> 1, "aze" -> "aze")).getOrElse(this)
}

object JwtSession {
  lazy val HEADER_NAME: String =
    Play.maybeApplication.flatMap(_.configuration.getString("session.jwtName")).getOrElse("Authorization")

  lazy val MAX_AGE: Option[Long] =
    Play.maybeApplication.flatMap(_.configuration.getMilliseconds("session.maxAge"))

  lazy val ALGORITHM: String =
    Play.maybeApplication.flatMap(_.configuration.getString("session.algorithm")).getOrElse("HmacSHA256")

  private def key =
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret"))

  def deserialize(token: String): JwtSession =
    JwtJson.decodeAllJson(token, key).map { tuple =>
      JwtSession(tuple._1, tuple._2, tuple._3)
    }.getOrElse(JwtSession())

  def defaultHeader: JwtHeader = JwtHeader(algorithm = Option(ALGORITHM), typ = Option("JWT"))
  def defaultClaim: JwtClaim = MAX_AGE match {
    case Some(seconds) => JwtClaim().expiresIn(1000 * seconds)
    case _ => JwtClaim()
  }

  private def asJsObject[A](value: A)(implicit writer: Writes[A]): JsObject = writer.writes(value) match {
    case value: JsObject => value
    case _ => Json.obj()
  }

  def apply: JwtSession = JwtSession.apply(defaultHeader, defaultClaim)

  def apply(jsClaim: JsObject): JwtSession =
    JwtSession.apply(asJsObject(defaultHeader), jsClaim)

  def apply(fields: (String, JsValueWrapper)*): JwtSession = JwtSession.apply(Json.obj(fields: _*))

  def apply(jsHeader: JsObject, jsClaim: JsObject): JwtSession =
    new JwtSession(jsHeader, jsClaim, None)

  def apply(claim: JwtClaim): JwtSession = JwtSession.apply(defaultHeader, claim)

  def apply(header: JwtHeader, claim: JwtClaim): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), None)
}
