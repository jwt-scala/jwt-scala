package pdi.jwt

import java.time.Clock

import argonaut.*
import argonaut.Argonaut.*
import pdi.jwt.exceptions.{JwtNonStringException, JwtValidationException}

trait JwtArgonautParser[H, C] extends JwtJsonCommon[Json, H, C] {
  override protected def parse(value: String): Json = Parse.parseOption(value).get

  override protected def stringify(value: Json): String = value.nospaces

  override protected def getAlgorithm(header: Json): Option[JwtAlgorithm] = header
    .field("alg")
    .map(_.stringOrEmpty)
    .filterNot(_ == "none")
    .map(JwtAlgorithm.fromString)

  def parseJwk(json: String): Jwk = readJwk(
    parse(json).obj.getOrElse(throw new JwtValidationException("A JWK must be a JSON object."))
  )

  def parseJwkSet(json: String): JwkSet = {
    val keys = parse(json)
      .field("keys")
      .flatMap(_.array)
      .getOrElse(throw new JwtValidationException("A JWK Set must have a [keys] array."))
    JwtJsonCommon.readJwkSetKeys(keys.iterator.flatMap(_.obj))(readJwk)
  }

  private def readJwk(json: JsonObject): Jwk = Jwk.fromMembers(
    name => json(name).map(_.string.getOrElse(throw new JwtNonStringException(name))),
    name =>
      json(name).map { value =>
        value.array
          .map(array => array.flatMap(_.string) -> array.length)
          .collect { case (strings, length) if strings.length == length => strings }
          .getOrElse(throw new JwtNonStringException(name))
      }
  )
}

object JwtArgonaut extends JwtArgonaut(Clock.systemUTC) {
  def apply(clock: Clock): JwtArgonaut = new JwtArgonaut(clock)

}

class JwtArgonaut(override val clock: Clock) extends JwtArgonautParser[JwtHeader, JwtClaim] {

  override protected def parseClaim(claim: String): JwtClaim = parseClaimHelp(claim)

  override protected def parseHeader(header: String): JwtHeader = parseHeaderHelp(header)

  implicit class ExtractJsonFieldToType(json: Json) {

    def -|>[T](field: String)(f: Json => T): Option[T] =
      (json -| field).map(f)

    def -|>>(field: String): Option[String] =
      this.-|>(field)(_.stringOrEmpty)

    def -||>(field: String): Option[Long] =
      this.-|>(field)(_.nospaces.toLong)

    def -|||(field: String): Option[Set[String]] =
      this.-|>(field)(_.arrayOrEmpty.map(_.nospaces).toSet)
  }

  private def parseClaimHelp(claim: String): JwtClaim =
    Parse.parseOption(claim) match {
      case Some(value) => jsonToJwtClaim(value)
      case None        => JwtClaim()
    }

  private def parseHeaderHelp(header: String): JwtHeader =
    Parse.parseOption(header).map(jsonToJwtHeader) match {
      case Some(value) => value
      case None        => JwtHeader(None)
    }

  private def jsonToJwtHeader(json: Json): JwtHeader = {
    val alg = getAlgorithm(json)
    val typ = json -|>> "typ"
    val contentType = json -|>> "cty"
    val keyId = json -|>> "kid"
    JwtHeader(alg, typ, contentType, keyId)
  }

  private val jwtSpecificFieldNames = List("iss", "sub", "aud", "exp", "nbf", "iat", "jti")

  private def jsonToJwtClaim(json: Json): JwtClaim = {
    val issuer = json -|>> "iss"
    val subject = json -|>> "sub"
    val audience = json -||| "aud"
    val expiration = json -||> "exp"
    val notBefore = json -||> "nbf"
    val issuedAt = json -||> "iat"
    val jwtId = json -|>> "jti"
    val content = json.objectFieldsOrEmpty
      .filterNot(jwtSpecificFieldNames.contains)
      .map { field =>
        (field, (json -| field).get)
      }
      .foldRight(jEmptyObject) { case ((fieldName, field), obj) =>
        (fieldName := field) ->: obj
      }
    JwtClaim(content.nospaces, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }
}
