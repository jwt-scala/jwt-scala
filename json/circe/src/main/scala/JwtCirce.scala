package pdi.jwt

import io.circe._
import io.circe.syntax._
import io.circe.jawn.{parse => jawnParse}
import java.time.Clock

import scala.annotation.nowarn
import scala.util.{Failure, Try, Success}

/** Implementation of `JwtCore` using `Json` from Circe.
  */
trait JwtCirceParser[H, C] extends JwtJsonCommon[Json, H, C] {
  protected def parse(value: String): Try[Json] = jawnParse(value).fold(Failure(_), Success(_))
  protected def stringify(value: Json): String = value.asJson.noSpaces
  protected def getAlgorithm(header: Json): Option[JwtAlgorithm] = getAlg(header.hcursor)

  protected def getAlg(cursor: HCursor): Option[JwtAlgorithm] = cursor
    .get[String]("alg")
    .toOption
    .filterNot(_ == "none")
    .map(JwtAlgorithm.fromString)
}

object JwtCirce extends JwtCirceParser[JwtHeader, JwtClaim] {
  def apply(clock: Clock): JwtCirce = new JwtCirce(clock)

  def parseHeader(header: String): Try[JwtHeader] = parseHeaderHelp(header)
  def parseClaim(claim: String): Try[JwtClaim] = parseClaimHelp(claim)

  private def parseHeaderHelp(header: String): Try[JwtHeader] = {
    parse(header).map(_.hcursor).map { cursor =>
      JwtHeader(
        algorithm = getAlg(cursor),
        typ = cursor.get[String]("typ").toOption,
        contentType = cursor.get[String]("cty").toOption,
        keyId = cursor.get[String]("kid").toOption
      )
    }
  }

  @nowarn // The cats import is necessary for 2.12 but not for 2.13, causing a warning
  private def parseClaimHelp(claim: String): Try[JwtClaim] = {
    import cats.syntax.either._

    parse(claim).map(_.hcursor).map { cursor =>
      val contentCursor =
        List("iss", "sub", "aud", "exp", "nbf", "iat", "jti").foldLeft(cursor) { (cursor, field) =>
          cursor.downField(field).delete.success match {
            case Some(newCursor) => newCursor
            case None            => cursor
          }
        }
      JwtClaim(
        content = contentCursor.top.asJson.noSpaces,
        issuer = cursor.get[String]("iss").toOption,
        subject = cursor.get[String]("sub").toOption,
        audience = cursor
          .get[Set[String]]("aud")
          .orElse(cursor.get[String]("aud").map(s => Set(s)))
          .toOption,
        expiration = cursor.get[Long]("exp").toOption,
        notBefore = cursor.get[Long]("nbf").toOption,
        issuedAt = cursor.get[Long]("iat").toOption,
        jwtId = cursor.get[String]("jti").toOption
      )
    }
  }
}

class JwtCirce private (override val clock: Clock) extends JwtCirceParser[JwtHeader, JwtClaim] {
  import JwtCirce.{parseHeaderHelp, parseClaimHelp}

  def parseHeader(header: String): Try[JwtHeader] = parseHeaderHelp(header)

  def parseClaim(claim: String): Try[JwtClaim] = parseClaimHelp(claim)
}
