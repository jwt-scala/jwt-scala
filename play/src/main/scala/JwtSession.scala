package pdi.jwt

import play.api.Play
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper

/** Similar to the default Play Session but using JsObject instead of Map[String, String]. The data is separated into two attributes: 
  * `headerData` and `claimData`. There is also a optional signature. Most of the time, you should only care about the `claimData` which
  * stores the claim of the token containing the custom values you eventually put in it. That's why all methods of `JwtSession` (such as
  * add and removing values) only modifiy the `claimData`.
  *
  * To see a full list of samples, check the [[http://pauldijou.fr/jwt-scala/samples/jwt-play/ online documentation]].
  *
  * '''Warning''' Be aware that if you override the `claimData` (using `withClaim` for example), you might override some attributes that 
  * were automatically put inside the claim such as the expiration of the token.
  *
  */
case class JwtSession(
  headerData: JsObject,
  claimData: JsObject,
  signature: Option[String]
) {
  /** Merge the `value` with `claimData` */
  def + (value: JsObject): JwtSession = this.copy(claimData = claimData.deepMerge(value))

  /** Add this (key, value) to `claimData` (existing key will be overriden) */
  def + (key: String, value: JsValueWrapper): JwtSession = this + Json.obj(key -> value)

  /** Convert `value` to its JSON counterpart and add it to `claimData` */
  def + [T](key: String, value: T)(implicit writer: Writes[T]): JwtSession = this + Json.obj(key -> writer.writes(value))

  /** Add a sequence of (key, value) to `claimData` */
  def ++ (fields: (String, JsValueWrapper)*): JwtSession = this + Json.obj(fields: _*)

  /** Remove one key from `claimData` */
  def - (fieldName: String): JwtSession = this.copy(claimData = claimData - fieldName)

  /** Remove a sequence of keys from `claimData` */
  def -- (fieldNames: String*): JwtSession = this.copy(claimData = fieldNames.foldLeft(claimData) {
    (data, fieldName) => (data - fieldName)
  })

  /** Retrieve the value corresponding to `fieldName` from `claimData` */
  def get(fieldName: String): JsValue = claimData \ fieldName

  /** After retrieving the value, try to read it as T, if no value or fails, returns None. */
  def getAs[T](fieldName: String)(implicit reader: Reads[T]): Option[T] = reader.reads(get(fieldName)).asOpt

  /** Alias of `get` */
  def apply(fieldName: String): JsValue = get(fieldName)

  lazy val isEmpty: Boolean = claimData.keys.isEmpty

  def claim: JwtClaim = jwtClaimReader.reads(claimData).get
  def header: JwtHeader = jwtHeaderReader.reads(headerData).get

  /** Encode the session as a JSON Web Token */
  def serialize: String = JwtJson.encode(headerData, claimData, JwtSession.key)

  /** Overrride the `claimData` */
  def withClaim(claim: JwtClaim): JwtSession = this.copy(claimData = JwtSession.asJsObject(claim))
  
  /** Override the `headerData` */
  def withHeader(header: JwtHeader): JwtSession = this.copy(headerData = JwtSession.asJsObject(header))

  /** Override the `signature` (seriously, you should never need this method) */
  def withSignature(signature: Option[String]): JwtSession = this.copy(signature = signature)

  /** If your Play app config has a `session.maxAge`, it will extend the expiration by that amount */
  def refresh: JwtSession = JwtSession.MAX_AGE.map(sec => this + ("exp", JwtTime.nowSeconds + sec)).getOrElse(this)
}

object JwtSession {
  lazy val HEADER_NAME: String =
    Play.maybeApplication.flatMap(_.configuration.getString("session.jwtName")).getOrElse("Authorization")

  lazy val MAX_AGE: Option[Long] =
    Play.maybeApplication.flatMap(_.configuration.getMilliseconds("session.maxAge").map(_ / 1000))

  lazy val ALGORITHM: JwtAlgorithm =
    Play.maybeApplication
      .flatMap(_.configuration.getString("session.algorithm").map(JwtAlgorithm.fromString))
      .getOrElse(JwtAlgorithm.HmacSHA256)

  lazy val TOKEN_PREFIX: String =
    Play.maybeApplication.flatMap(_.configuration.getString("session.tokenPrefix")).getOrElse("Bearer ")

  private def key: Option[String] =
    Play.maybeApplication.flatMap(_.configuration.getString("application.secret"))

  def deserialize(token: String): JwtSession =
    JwtJson.decodeJsonAll(token, key).map { tuple =>
      JwtSession(tuple._1, tuple._2, tuple._3)
    }.getOrElse(JwtSession())

  private def asJsObject[A](value: A)(implicit writer: Writes[A]): JsObject = writer.writes(value) match {
    case value: JsObject => value
    case _ => Json.obj()
  }

  def defaultHeader: JwtHeader = key.map(_ => JwtHeader(ALGORITHM)).getOrElse(JwtHeader())

  def defaultClaim: JwtClaim = MAX_AGE match {
    case Some(seconds) => JwtClaim().expiresIn(seconds)
    case _ => JwtClaim()
  }

  def apply(jsClaim: JsObject): JwtSession =
    JwtSession.apply(asJsObject(defaultHeader), jsClaim)

  def apply(fields: (String, JsValueWrapper)*): JwtSession =
    if (fields.isEmpty) {
      JwtSession.apply(defaultHeader, defaultClaim)
    } else {
      JwtSession.apply(Json.obj(fields: _*))
    }

  def apply(jsHeader: JsObject, jsClaim: JsObject): JwtSession =
    new JwtSession(jsHeader, jsClaim, None)

  def apply(claim: JwtClaim): JwtSession =
    JwtSession.apply(defaultHeader, claim)

  def apply(header: JwtHeader, claim: JwtClaim): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), None)

  def apply(header: JwtHeader, claim: JwtClaim, signature: Option[String]): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), signature)

  def apply(header: JwtHeader, claim: JwtClaim, signature: String): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), Option(signature))
}
