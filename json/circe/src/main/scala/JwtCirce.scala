package pdi.jwt

import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import io.circe.jawn.{parse => jawnParse}

import pdi.jwt.exceptions.JwtNonStringException


/**
  * Implementation of `JwtCore` using `Json` from Circe.
  */
trait JwtCirceParser[H, C] extends JwtJsonCommon[Json, H, C] {
  protected def parse(value: String): Json = jawnParse(value).toOption.get
  protected def stringify(value: Json): String = value.asJson.noSpaces
  protected def getAlgorithm(header: Json): Option[JwtAlgorithm] = getAlg(header.hcursor)

  protected def getAlg(cursor: HCursor): Option[JwtAlgorithm] = {
    cursor.get[String]("alg").toOption.flatMap {
      case "none" => None
      case s if s == null => None
      case s: String => Option(JwtAlgorithm.fromString(s))
      case _ => throw new JwtNonStringException("alg")
    }
  }
}

object JwtCirce extends JwtCirceParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = {
    val cursor = parse(header).hcursor
    JwtHeader(
        algorithm = getAlg(cursor)
      , typ = cursor.get[String]("typ").toOption
      , contentType = cursor.get[String]("cty").toOption
      , keyId = cursor.get[String]("kid").toOption
    )
  }
  protected def parseClaim(claim: String): JwtClaim = {
    val cursor = parse(claim).hcursor
    val contentCursor = List("iss", "sub", "aud", "exp", "nbf", "iat", "jti").foldLeft(cursor) { (cursor, field) =>
      cursor.downField(field).delete.success match {
        case Some(newCursor) => newCursor
        case None => cursor
      }
    }
    JwtClaim(
        content = contentCursor.top.asJson.noSpaces
      , issuer = cursor.get[String]("iss").toOption
      , subject = cursor.get[String]("sub").toOption
      , audience = cursor.get[Set[String]]("aud").orElse(cursor.get[String]("aud").map(s => Set(s))).toOption
      , expiration = cursor.get[Long]("exp").toOption
      , notBefore = cursor.get[Long]("nbf").toOption
      , issuedAt = cursor.get[Long]("iat").toOption
      , jwtId = cursor.get[String]("jti").toOption
    )
  }
}
