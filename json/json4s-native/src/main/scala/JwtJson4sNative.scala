package pdi.jwt

import java.time.Clock
import scala.util.{Failure, Try, Success}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.JsonMethods.{parse => jparse}
import org.json4s.native.Serialization

/** Implementation of `JwtCore` using `JObject` from Json4s Native.
  *
  * To see a full list of samples, check the [[https://jwt-scala.github.io/jwt-scala/jwt-json4s.html online documentation]].
  */
trait JwtJson4sParser[H, C] extends JwtJson4sCommon[H, C] with JwtJson4sImplicits {
  protected def parse(value: String): Try[JObject] = Try(jparse(value)).flatMap {
    case res: JObject => Success(res)
    case _            => Failure(new RuntimeException(s"Couldn't parse [$value] to a JObject"))
  }

  protected def stringify(value: JObject): String = compact(render(value))

  protected implicit val formats = Serialization.formats(NoTypeHints)
}

object JwtJson4s extends JwtJson4sParser[JwtHeader, JwtClaim] {
  def apply(clock: Clock): JwtJson4s = new JwtJson4s(clock)
  def parseHeader(header: String): Try[JwtHeader] = parse(header).flatMap(readHeader)
  def parseClaim(claim: String): Try[JwtClaim] = parse(claim).flatMap(readClaim)
}

class JwtJson4s private (override val clock: Clock) extends JwtJson4sParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): Try[JwtHeader] = parse(header).flatMap(readHeader)
  def parseClaim(claim: String): Try[JwtClaim] = parse(claim).flatMap(readClaim)
}
