package pdi.jwt

import io.circe._
import io.circe.syntax._
import io.circe.jawn.{parse => jawnParse}

import pdi.jwt.exceptions.JwtNonStringException


/**
  * Implementation of `JwtCore` using `Json` from Circe.
  */
object JwtCirce extends JwtJsonCommon[Json] {
  protected def parse(value: String): Json = jawnParse(value).toOption.get
  protected def parseClaim(claim: String): JwtClaim = {
    val cursor = parse(claim).hcursor
    val contentCursor = List("iss", "sub", "aud", "exp", "nbf", "iat", "jti").foldLeft(cursor) { (cursor, field) =>
      val newCursor = cursor.downField(field).delete
      if(newCursor.succeeded) newCursor.any
      else cursor
    }
    JwtClaim(
        content = contentCursor.top.asJson.noSpaces
      , issuer = cursor.get[String]("iss").toOption
      , subject = cursor.get[String]("sub").toOption
      , audience = cursor.get[String]("aud").toOption
      , expiration = cursor.get[Long]("exp").toOption
      , notBefore = cursor.get[Long]("nbf").toOption
      , issuedAt = cursor.get[Long]("iat").toOption
      , jwtId = cursor.get[String]("jti").toOption
    )
  }

  protected def stringify(value: Json): String = value.asJson.noSpaces

  private def getAlg(cursor: HCursor): Option[JwtAlgorithm] = {
    cursor.get[String]("alg").toOption.flatMap {
      case "none" => None
      case s if s == null => None
      case s: String => Option(JwtAlgorithm.fromString(s))
      case _ => throw new JwtNonStringException("alg")
    }
  }

  protected def parseHeader(header: String): JwtHeader = {
    val cursor = parse(header).hcursor
    JwtHeader(
        algorithm = getAlg(cursor)
      , typ = cursor.get[String]("typ").toOption
      , contentType = cursor.get[String]("cty").toOption
    )
  }

  protected def getAlgorithm(header: Json): Option[JwtAlgorithm] = getAlg(header.hcursor)

}
