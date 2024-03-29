package pdi.jwt

import java.time.Clock

import upickle.default.*

/** Implementation of `JwtCore` using `Js.Value` from uPickle.
  *
  * To see a full list of samples, check the
  * [[https://jwt-scala.github.io/jwt-scala/jwt-upickle.html online documentation]].
  */
trait JwtUpickleParser[H, C] extends JwtJsonCommon[ujson.Value, H, C] with JwtUpickleImplicits {
  protected def parse(value: String): ujson.Value = ujson.read(value)

  protected def stringify(value: ujson.Value): String = ujson.write(value)

  protected def getAlgorithm(header: ujson.Value): Option[JwtAlgorithm] = header match {
    case obj: ujson.Obj =>
      val fields = obj.value.toMap
      fields.get("alg").flatMap(alg => JwtAlgorithm.optionFromString(alg.str.toString()))

    case _ => None
  }
}

object JwtUpickle extends JwtUpickle(Clock.systemUTC) {
  def apply(clock: Clock): JwtUpickle = new JwtUpickle(clock)
}

class JwtUpickle(override val clock: Clock) extends JwtUpickleParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = read[JwtHeader](header)
  def parseClaim(claim: String): JwtClaim = read[JwtClaim](claim)
}
