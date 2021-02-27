package pdi.jwt

import java.time.Clock
import pdi.jwt.exceptions.JwtNonStringException
import spray.json._

/** Implementation of `JwtCore` using `JsObject` from spray-json.
  */
trait JwtSprayJsonParser[H, C] extends JwtJsonCommon[JsObject, H, C] {
  protected def parse(value: String): JsObject = value.parseJson.asJsObject

  protected def stringify(value: JsObject): String = value.compactPrint

  protected def getAlgorithm(header: JsObject): Option[JwtAlgorithm] =
    header.fields.get("alg").flatMap {
      case JsString("none") => None
      case JsString(algo)   => Option(JwtAlgorithm.fromString(algo))
      case JsNull           => None
      case _                => throw new JwtNonStringException("alg")
    }

}

object JwtSprayJson extends JwtSprayJsonParser[JwtHeader, JwtClaim] {
  import DefaultJsonProtocol._

  def apply(clock: Clock): JwtSprayJson = new JwtSprayJson(clock)

  override def parseHeader(header: String): JwtHeader = {
    val jsObj = parse(header)
    JwtHeader(
      algorithm = getAlgorithm(jsObj),
      typ = safeGetField[String](jsObj, "typ"),
      contentType = safeGetField[String](jsObj, "cty"),
      keyId = safeGetField[String](jsObj, "kid")
    )
  }

  override def parseClaim(claim: String): JwtClaim = {
    val jsObj = parse(claim)
    val content = JsObject(
      jsObj.fields - "iss" - "sub" - "aud" - "exp" - "nbf" - "iat" - "jti"
    )
    JwtClaim(
      content = stringify(content),
      issuer = safeGetField[String](jsObj, "iss"),
      subject = safeGetField[String](jsObj, "sub"),
      audience = safeGetField[Set[String]](jsObj, "aud")
        .orElse(safeGetField[String](jsObj, "aud").map(s => Set(s))),
      expiration = safeGetField[Long](jsObj, "exp"),
      notBefore = safeGetField[Long](jsObj, "nbf"),
      issuedAt = safeGetField[Long](jsObj, "iat"),
      jwtId = safeGetField[String](jsObj, "jti")
    )
  }

  private[this] def safeRead[A: JsonReader](js: JsValue) =
    safeReader[A].read(js).fold(_ => None, a => Option(a))

  private[this] def safeGetField[A: JsonReader](js: JsObject, name: String) =
    js.fields.get(name).flatMap(safeRead[A])
}

class JwtSprayJson private (override val clock: Clock)
    extends JwtSprayJsonParser[JwtHeader, JwtClaim] {
  override def parseHeader(header: String): JwtHeader = JwtSprayJson.parseHeader(header)
  override def parseClaim(header: String): JwtClaim = JwtSprayJson.parseClaim(header)
}
