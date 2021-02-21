package pdi.jwt

import io.circe._
import io.circe.syntax._
import io.circe.jawn.{parse => jawnParse}
import java.time.Clock

import pdi.jwt.exceptions.JwtNonStringException
import scala.annotation.nowarn

/** Implementation of `JwtCore` using `Json` from Circe.
  */
trait JwtCirceParser[H, C] extends JwtJsonCommon[Json, H, C] {
  protected def parse(value: String): Json = jawnParse(value).fold(throw _, identity)
  protected def stringify(value: Json): String = value.asJson.noSpaces
  protected def getAlgorithm(header: Json): Option[JwtAlgorithm] = getAlg(header.hcursor)

  protected def getAlg(cursor: HCursor): Option[JwtAlgorithm] = {
    cursor.get[String]("alg").toOption.flatMap {
      case "none"         => None
      case s if s == null => None
      case s: String      => Option(JwtAlgorithm.fromString(s))
      case null           => throw new JwtNonStringException("alg")
    }
  }
}

object JwtCirce extends JwtCirceParser[JwtHeader, JwtClaim] {
  def apply(clock: Clock): JwtCirce = new JwtCirce(clock)

  def parseHeader(header: String): JwtHeader = parseHeaderHelp(header)
  def parseClaim(claim: String): JwtClaim = parseClaimHelp(claim)

  private def parseHeaderHelp(header: String): JwtHeader = {
    val cursor = parse(header).hcursor
    JwtHeader(
      algorithm = getAlg(cursor),
      typ = cursor.get[String]("typ").toOption,
      contentType = cursor.get[String]("cty").toOption,
      keyId = cursor.get[String]("kid").toOption
    )
  }

  @nowarn // The cats import is necessary for 2.12 but not for 2.13, causing a warning
  private def parseClaimHelp(claim: String): JwtClaim = {
    import cats.syntax.either._

    val cursor = parse(claim).hcursor
    val contentCursor = List("iss", "sub", "aud", "exp", "nbf", "iat", "jti").foldLeft(cursor) {
      (cursor, field) =>
        cursor.downField(field).delete.success match {
          case Some(newCursor) => newCursor
          case None            => cursor
        }
    }
    JwtClaim(
      content = contentCursor.top.asJson.noSpaces,
      issuer = cursor.get[String]("iss").toOption,
      subject = cursor.get[String]("sub").toOption,
      audience =
        cursor.get[Set[String]]("aud").orElse(cursor.get[String]("aud").map(s => Set(s))).toOption,
      expiration = cursor.get[Long]("exp").toOption,
      notBefore = cursor.get[Long]("nbf").toOption,
      issuedAt = cursor.get[Long]("iat").toOption,
      jwtId = cursor.get[String]("jti").toOption
    )
  }
}

class JwtCirce private (override val clock: Clock) extends JwtCirceParser[JwtHeader, JwtClaim] {
  import JwtCirce.{parseHeaderHelp, parseClaimHelp}

  def parseHeader(header: String): JwtHeader = parseHeaderHelp(header)

  def parseClaim(claim: String): JwtClaim = parseClaimHelp(claim)
}
