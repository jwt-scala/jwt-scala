package pdi.jwt

import scala.util.Try

import org.json4s.*
import pdi.jwt.exceptions.{
  JwtNonNumberException,
  JwtNonStringException,
  JwtNonStringSetOrStringException,
  JwtValidationException
}

trait JwtJson4sCommon[H, C] extends JwtJsonCommon[JObject, H, C] {
  protected def getAlgorithm(header: JObject): Option[JwtAlgorithm] = header \ "alg" match {
    case JString("none") => None
    case JString(algo)   => Option(JwtAlgorithm.fromString(algo))
    case JNull           => None
    case JNothing        => None
    case _               => throw new JwtNonStringException("alg")
  }

  def readClaim(json: JValue): JwtClaim = json match {
    case value: JObject =>
      JwtClaim.apply(
        issuer = extractString(value, "iss"),
        subject = extractString(value, "sub"),
        audience = extractStringSetOrString(value, "aud"),
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
    case value: JObject =>
      JwtHeader.apply(
        algorithm = extractString(value, "alg").flatMap(JwtAlgorithm.optionFromString),
        typ = extractString(value, "typ"),
        contentType = extractString(value, "cty"),
        keyId = extractString(value, "kid")
      )
    case _ => throw new RuntimeException("Expected a JObject")
  }

  def writeHeader(header: JwtHeader): JValue = parse(header.toJson)

  protected def extractString(json: JObject, fieldName: String): Option[String] =
    (json \ fieldName) match {
      case JString(value) => Option(value)
      case JNull          => None
      case JNothing       => None
      case _              => throw new JwtNonStringException(fieldName)
    }

  protected def readJwk(json: JValue): Jwk = json match {
    case obj: JObject => readJwkObject(obj)
    case _            => throw new JwtValidationException("A JWK must be a JSON object.")
  }

  protected def readJwkSet(json: JValue): JwkSet = {
    val keys = json match {
      case JObject(fields) => fields.collectFirst { case ("keys", JArray(keys)) => keys }
      case _               => None
    }
    JwkSet(
      keys
        .getOrElse(throw new JwtValidationException("A JWK Set must have a [keys] array."))
        .collect { case key: JObject => key }
        .flatMap(key => Try(readJwkObject(key)).toOption)
    )
  }

  private def readJwkObject(json: JObject): Jwk = Jwk.fromMembers(
    name =>
      json.obj.collectFirst { case (`name`, value) => value }.map {
        case JString(value) => value
        case _              => throw new JwtNonStringException(name)
      },
    name =>
      json.obj.collectFirst { case (`name`, value) => value }.map {
        case JArray(elements) =>
          val strings = elements.collect { case JString(value) => value }
          if (strings.length == elements.length) strings
          else throw new JwtNonStringException(name)
        case _ => throw new JwtNonStringException(name)
      }
  )

  protected def extractStringSetOrString(json: JObject, fieldName: String): Option[Set[String]] =
    (json \ fieldName) match {
      case JString(value) => Option(Set(value))
      case JArray(_)      =>
        try {
          (json \ fieldName) match {
            case JArray(values) => Some(values.map(_.asInstanceOf[JString].s).toSet)
            case _              => None
          }
        } catch {
          case _: MappingException => throw new JwtNonStringSetOrStringException(fieldName)
        }
      case JNull    => None
      case JNothing => None
      case _        => throw new JwtNonStringSetOrStringException(fieldName)
    }

  protected def extractLong(json: JObject, fieldName: String): Option[Long] =
    (json \ fieldName) match {
      case JInt(value) => Option(value.toLong)
      case JNull       => None
      case JNothing    => None
      case _           => throw new JwtNonNumberException(fieldName)
    }

  protected def filterClaimFields(json: JObject): JObject = json.removeField {
    case JField("iss", _) => true
    case JField("sub", _) => true
    case JField("aud", _) => true
    case JField("exp", _) => true
    case JField("nbf", _) => true
    case JField("iat", _) => true
    case JField("jti", _) => true
    case _                => false
  } match {
    case res: JObject => res
    case _            =>
      throw new RuntimeException(
        "How did we manage to go from JObject to something else by just removing fields?"
      )
  }
}
