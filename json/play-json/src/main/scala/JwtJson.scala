package pdi.jwt

import java.time.Clock

import pdi.jwt.exceptions.JwtNonStringException
import play.api.libs.json._

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
}

class JwtJson private (override val clock: Clock) extends JwtJsonParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = jwtPlayJsonHeaderReader.reads(Json.parse(header)).get
  def parseClaim(claim: String): JwtClaim = jwtPlayJsonClaimReader.reads(Json.parse(claim)).get
}
