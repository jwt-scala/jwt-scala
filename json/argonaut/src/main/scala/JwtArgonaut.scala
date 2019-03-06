package pdi.jwt

import scala.language.postfixOps

import argonaut.Argonaut._
import argonaut._
import pdi.jwt.exceptions.JwtNonStringException

trait JwtArgonautParser[H <: JwtHeader, C <: JwtClaim] extends JwtJsonCommon[Json, H, C] {
  override protected def parse(value: String): Json = Parse.parseOption(value).get

  override protected def stringify(value: Json): String = value.nospaces

  override protected def getAlgorithm(header: Json): Option[JwtAlgorithm] =
    header -| "alg" map (_.stringOrEmpty) flatMap {
      case "none" ⇒ None
      case alg: String ⇒ Some(JwtAlgorithm.fromString(alg))
      case _ ⇒ throw new JwtNonStringException("alg")
    }
}


object JwtArgonaut extends JwtArgonautParser[JwtHeader, JwtClaim] {

  implicit class ExtractJsonFieldToType(json: Json) {

    def -|>[T](field: String)(f: Json ⇒ T): Option[T] =
      json -| field map f

    def -|>>(field: String): Option[String] =
      (json -|> field) (_.stringOrEmpty)

    def -||>(field: String): Option[Long] =
      (json -|> field) (_.nospaces.toLong)

    def -|||(field: String): Option[Set[String]] =
      (json -|> field) (_.arrayOrEmpty.map(_.nospaces).toSet)
  }

  private def jsonToJwtHeader(json: Json): JwtHeader = {
    val alg = getAlgorithm(json)
    val typ = json -|>> "typ"
    val contentType = json -|>> "cty"
    val keyId = json -|>> "kid"
    JwtHeader(alg, typ, contentType, keyId)
  }

  override protected def parseHeader(header: String): JwtHeader =
    Parse.parseOption(header) map jsonToJwtHeader match {
      case Some(value) ⇒ value
      case None ⇒ JwtHeader(None)
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
    val content = json.objectFieldsOrEmpty.filterNot(jwtSpecificFieldNames.contains).map { field ⇒
      (field, (json -| field).get)
    }.foldRight(jEmptyObject) { case ((fieldName, field), obj) ⇒
      (fieldName := field) ->: obj
    }
    JwtClaim(content.nospaces, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  override protected def parseClaim(claim: String): JwtClaim =
    Parse.parseOption(claim) match {
      case Some(value) ⇒ jsonToJwtClaim(value)
      case None ⇒ JwtClaim()
    }
}
