package pdi.scala.jwt

import play.api.Play
import play.api.libs.json.{Json, JsValue, JsObject, Reads}

/*import pdi.scala.jwt.{jwtClaimReader, jwtHeaderReader}*/

case class JwtSession(
  claimData: JsObject = Json.obj(),
  headerData: JsObject = Json.obj(),
  signature: Option[String] = None
) {
  def + (value: JsObject): JwtSession = this.copy(claimData = claimData.deepMerge(value))

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
}

object JwtSession {
  lazy val HEADER_NAME: String =
    Play.maybeApplication.flatMap(_.configuration.getString("session.jwtName")).getOrElse("Authorization")

  lazy val maxAge: Option[Long] =
    Play.maybeApplication.flatMap(_.configuration.getMilliseconds("session.maxAge"))

  private def key =
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret"))

  def deserialize(token: String): JwtSession =
    JwtJson.decodeAllJson(token, key).map { tuple =>
      JwtSession(tuple._2, tuple._1, tuple._3)
    }.getOrElse(new JwtSession())

}
