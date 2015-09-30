package pdi.jwt

import org.json4s._
import org.json4s.JsonDSL.WithBigDecimal._

import pdi.jwt.exceptions.{JwtNonStringException, JwtNonNumberException}

trait JwtJson4sCommon extends JwtJsonCommon[JObject] {
  protected def getAlgorithm(header: JObject): Option[JwtAlgorithm] = header \ "alg" match {
    case JString("none") => None
    case JString(algo) => Option(JwtAlgorithm.fromString(algo))
    case JNull => None
    case JNothing => None
    case _ => throw new JwtNonStringException("alg")
  }

  def readClaim(json: JValue): JwtClaim = json match {
    case value: JObject => JwtClaim.apply(
      issuer = extractString(value, "iss"),
      subject = extractString(value, "sub"),
      audience = extractString(value, "aud"),
      expiration = extractLong(value, "exp"),
      notBefore = extractLong(value, "nbf"),
      issuedAt = extractLong(value, "iat"),
      jwtId = extractString(value, "jti"),
      content = stringify(filterClaimFields(value))
    )
    case _ => throw new RuntimeException("Expected a JObject")
  }

  def writeClaim(claim: JwtClaim): JValue = parse(claim.toJson)

  def readHeader(json: JValue): JwtHeader = json match {
    case value: JObject => JwtHeader.apply(
      algorithm = extractString(value, "alg").flatMap(JwtAlgorithm.optionFromString),
      typ = extractString(value, "typ"),
      contentType = extractString(value, "cty")
    )
    case _ => throw new RuntimeException("Expected a JObject")
  }

  def writeHeader(header: JwtHeader): JValue = parse(header.toJson)

  private def extractString(json: JObject, fieldName: String): Option[String] = (json \ fieldName) match {
    case JString(value) => Option(value)
    case JNull => None
    case JNothing => None
    case _ => throw new JwtNonStringException(fieldName)
  }

  private def extractLong(json: JObject, fieldName: String): Option[Long] = (json \ fieldName) match {
    case JInt(value) => Option(value.toLong)
    case JNull => None
    case JNothing => None
    case _ => throw new JwtNonNumberException(fieldName)
  }

  private def filterClaimFields(json: JObject): JObject = json removeField {
    case JField("iss", _) => true
    case JField("sub", _) => true
    case JField("aud", _) => true
    case JField("exp", _) => true
    case JField("nbf", _) => true
    case JField("iat", _) => true
    case JField("jti", _) => true
    case _ => false
  } match {
    case res: JObject => res
    case _ => throw new RuntimeException("How did we manage to go from JObject to something else by just removing fields?")
  }
}
