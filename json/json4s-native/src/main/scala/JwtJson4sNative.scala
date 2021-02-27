package pdi.jwt

import java.time.Clock
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.JsonMethods.{parse => jparse}
import org.json4s.native.Serialization

/** Implementation of `JwtCore` using `JObject` from Json4s Native.
  *
  * To see a full list of samples, check the [[http://pauldijou.fr/jwt-scala/samples/jwt-json4s/ online documentation]].
  */
trait JwtJson4sParser[H, C] extends JwtJson4sCommon[H, C] with JwtJson4sImplicits {
  protected def parse(value: String): JObject = jparse(value) match {
    case res: JObject => res
    case _            => throw new RuntimeException(s"Couldn't parse [$value] to a JObject")
  }

  protected def stringify(value: JObject): String = compact(render(value))

  protected implicit val formats = Serialization.formats(NoTypeHints)
}

object JwtJson4s extends JwtJson4sParser[JwtHeader, JwtClaim] {
  def apply(clock: Clock): JwtJson4s = new JwtJson4s(clock)
  def parseHeader(header: String): JwtHeader = readHeader(parse(header))
  def parseClaim(claim: String): JwtClaim = readClaim(parse(claim))
}

class JwtJson4s private (override val clock: Clock) extends JwtJson4sParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = readHeader(parse(header))
  def parseClaim(claim: String): JwtClaim = readClaim(parse(claim))
}
