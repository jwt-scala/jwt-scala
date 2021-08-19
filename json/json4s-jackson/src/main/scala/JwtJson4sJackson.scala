package pdi.jwt

import java.time.Clock

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.JsonMethods.{parse => jparse}

/** Implementation of `JwtCore` using `JObject` from Json4s Jackson.
  *
  * To see a full list of samples, check the
  * [[https://jwt-scala.github.io/jwt-scala/jwt-json4s.html online documentation]].
  */
trait JwtJson4sParser[H, C] extends JwtJson4sCommon[H, C] with JwtJson4sImplicits {
  protected def parse(value: String): JObject = jparse(value) match {
    case res: JObject => res
    case _            => throw new RuntimeException(s"Couldn't parse [$value] to a JObject")
  }

  protected def stringify(value: JObject): String = compact(render(value))
}

object JwtJson4s extends JwtJson4s(Clock.systemUTC) {
  def apply(clock: Clock): JwtJson4s = new JwtJson4s(clock)
}

class JwtJson4s(override val clock: Clock) extends JwtJson4sParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = readHeader(parse(header))
  def parseClaim(claim: String): JwtClaim = readClaim(parse(claim))
}
