package pdi.jwt

import play.api.Play
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import pdi.jwt.algorithms.JwtHmacAlgorithm

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
  signature: String
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
  def get(fieldName: String): Option[JsValue] = (claimData \ fieldName).toOption

  /** After retrieving the value, try to read it as T, if no value or fails, returns None. */
  def getAs[T](fieldName: String)(implicit reader: Reads[T]): Option[T] =
    get(fieldName).flatMap(value => reader.reads(value).asOpt)

  /** Alias of `get` */
  def apply(fieldName: String): Option[JsValue] = get(fieldName)

  def isEmpty(): Boolean = claimData.keys.isEmpty

  def claim: JwtClaim = JwtSession.jwtPlayJsonClaimReader.reads(claimData).get
  def header: JwtHeader = JwtSession.jwtPlayJsonHeaderReader.reads(headerData).get

  /** Encode the session as a JSON Web Token */
  def serialize: String = JwtSession.key match {
    case Some(k) => JwtJson.encode(headerData, claimData, k)
    case _ => JwtJson.encode(headerData, claimData)
  }

  /** Overrride the `claimData` */
  def withClaim(claim: JwtClaim): JwtSession =
    this.copy(claimData = JwtSession.asJsObject(claim)(JwtSession.jwtPlayJsonClaimWriter))

  /** Override the `headerData` */
  def withHeader(header: JwtHeader): JwtSession =
    this.copy(headerData = JwtSession.asJsObject(header)(JwtSession.jwtPlayJsonHeaderWriter))

  /** Override the `signature` (seriously, you should never need this method) */
  def withSignature(signature: String): JwtSession = this.copy(signature = signature)

  /** If your Play app config has a `session.maxAge`, it will extend the expiration by that amount */
  def refresh(): JwtSession = JwtSession.MAX_AGE.map(sec => this + ("exp", JwtTime.nowSeconds + sec)).getOrElse(this)
}

object JwtSession extends JwtJsonImplicits with JwtPlayImplicits {
  // This half-hack is to fix a "bug" in Play Framework where Play assign "null"
  // values to missing keys leading to ConfigException.Null in Typesafe Config
  // Especially strange for the maxAge key. Not having it should mean no session timeout,
  // not crash my app.
  def wrap[T](getter: String => Option[T]): String => Option[T] = key => try {
    getter(key)
  } catch {
    case e: com.typesafe.config.ConfigException.Null => None
    case e: java.lang.RuntimeException => {
      e.getCause() match {
        case _: com.typesafe.config.ConfigException.Null => None
        case _ => throw e
      }
    }
  }

  val getConfigString = wrap[String](
    key => Play.maybeApplication.flatMap(_.configuration.getString(key))
  )

  val getConfigMillis = wrap[Long](
    key => Play.maybeApplication.flatMap(app =>
      if (app.configuration.has(key)) {
        Some(app.configuration.getMillis(key))
      } else {
        None
      }
    )
  )

  val REQUEST_HEADER_NAME: String = getConfigString("play.http.session.jwtName").getOrElse("Authorization")

  val RESPONSE_HEADER_NAME: String = getConfigString("play.http.session.jwtResponseName").getOrElse(REQUEST_HEADER_NAME)

  val MAX_AGE: Option[Long] = getConfigMillis("play.http.session.maxAge").map(_ / 1000)

  val ALGORITHM: JwtHmacAlgorithm =
    getConfigString("play.http.session.algorithm")
      .map(JwtAlgorithm.fromString)
      .flatMap {
        case algo: JwtHmacAlgorithm => Option(algo)
        case _ => throw new RuntimeException("You can only use HMAC algorithms for [play.http.session.algorithm]")
      }
      .getOrElse(JwtAlgorithm.HS256)

  val TOKEN_PREFIX: String = getConfigString("play.http.session.tokenPrefix").getOrElse("Bearer ")

  private def key: Option[String] = getConfigString("play.http.secret.key")

  def deserialize(token: String, options: JwtOptions): JwtSession = (key match {
      case Some(k) => JwtJson.decodeJsonAll(token, k, Seq(ALGORITHM), options)
      case _ => JwtJson.decodeJsonAll(token, options)
    }).map { tuple =>
      JwtSession(tuple._1, tuple._2, tuple._3)
    }.getOrElse(JwtSession())

  def deserialize(token: String): JwtSession = deserialize(token, JwtOptions.DEFAULT)

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
    new JwtSession(jsHeader, jsClaim, "")

  def apply(claim: JwtClaim): JwtSession =
    JwtSession.apply(defaultHeader, claim)

  def apply(header: JwtHeader, claim: JwtClaim): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), "")

  def apply(header: JwtHeader, claim: JwtClaim, signature: String): JwtSession =
    new JwtSession(asJsObject(header), asJsObject(claim), signature)
}
