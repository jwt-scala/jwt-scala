package pdi.jwt

import java.time.Clock
import scala.util.Try
import upickle.default._

/** Implementation of `JwtCore` using `Js.Value` from uPickle.
  *
  * To see a full list of samples, check the [[https://jwt-scala.github.io/jwt-scala/jwt-upickle.html online documentation]].
  */
trait JwtUpickleParser[H, C] extends JwtJsonCommon[ujson.Value, H, C] with JwtUpickleImplicits {
  protected def parse(value: String): Try[ujson.Value] = Try(ujson.read(value))

  protected def stringify(value: ujson.Value): String = ujson.write(value)

  protected def getAlgorithm(header: ujson.Value): Option[JwtAlgorithm] = header match {
    case obj: ujson.Obj =>
      val fields = obj.value.toMap
      fields.get("alg").flatMap(alg => JwtAlgorithm.optionFromString(alg.str.toString()))

    case _ => None
  }
}

object JwtUpickle extends JwtUpickleParser[JwtHeader, JwtClaim] {
  def apply(clock: Clock): JwtUpickle = new JwtUpickle(clock)
  def parseHeader(header: String): Try[JwtHeader] = Try(read[JwtHeader](header))
  def parseClaim(claim: String): Try[JwtClaim] = Try(read[JwtClaim](claim))
}

class JwtUpickle private (override val clock: Clock) extends JwtUpickleParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): Try[JwtHeader] = Try(read[JwtHeader](header))
  def parseClaim(claim: String): Try[JwtClaim] = Try(read[JwtClaim](claim))
}
