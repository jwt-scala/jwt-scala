package pdi.jwt

import java.time.Clock

import pdi.jwt.exceptions.{JwtNonNumberException, JwtNonStringException, JwtValidationException}
import play.api.libs.json.*

/** Implementation of `JwtCore` using `JsObject` from Play JSON.
  *
  * To see a full list of samples, check the
  * [[https://jwt-scala.github.io/jwt-scala/jwt-play-json.html online documentation]].
  */
trait JwtJsonParser[H, C] extends JwtJsonCommon[JsObject, H, C] with JwtJsonImplicits {
  protected def parse(value: String): JsObject = Json.parse(value).as[JsObject]

  protected def stringify(value: JsObject): String = Json.stringify(value)

  protected def getAlgorithm(header: JsObject): Option[JwtAlgorithm] =
    (header \ "alg").toOption.flatMap {
      case JsString("none") => None
      case JsString(algo)   => Option(JwtAlgorithm.fromString(algo))
      case JsNull           => None
      case _                => throw new JwtNonStringException("alg")
    }

}

object JwtJson extends JwtJson(Clock.systemUTC) {
  def apply(clock: Clock): JwtJson = new JwtJson(clock)

  // Play Json returns a useless exception on JsResult.get. We want to give more details about what's wrong in the exception.
  private[jwt] def jsErrorToException(error: JsError): Exception = error.errors.headOption
    .map { case (jsPath, errors) =>
      errors.headOption.map(_.message) match {
        case Some("error.expected.string") => new JwtNonStringException(jsPath.toString)
        case Some("error.expected.number") => new JwtNonNumberException(jsPath.toString)
        case _                             => new JwtValidationException(s"Failed to parse: $error")
      }
    }
    .getOrElse(new JwtValidationException(s"Failed to parse: $error"))
}

class JwtJson private (override val clock: Clock) extends JwtJsonParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = jwtPlayJsonHeaderReader
    .reads(Json.parse(header))
    .recoverTotal { e => throw JwtJson.jsErrorToException(e) }

  def parseClaim(claim: String): JwtClaim = jwtPlayJsonClaimReader
    .reads(Json.parse(claim))
    .recoverTotal { e => throw JwtJson.jsErrorToException(e) }
}
