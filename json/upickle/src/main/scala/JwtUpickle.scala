package pdi.jwt

import upickle.Js
import upickle.json
import upickle.default._

/**
  * Implementation of `JwtCore` using `Js.Value` from uPickle.
  *
  * To see a full list of samples, check the [[http://pauldijou.fr/jwt-scala/samples/jwt-upickle/ online documentation]].
  */
trait JwtUpickleParser[H, C] extends JwtJsonCommon[Js.Value, H, C] with JwtUpickleImplicits {
  protected def parse(value: String): Js.Value = json.read(value)

  protected def stringify(value: Js.Value): String = json.write(value)

  protected def getAlgorithm(header: Js.Value): Option[JwtAlgorithm] = header match {
    case obj: Js.Obj =>
      val fields = obj.value.toMap
      fields.get("alg").flatMap(alg => JwtAlgorithm.optionFromString(alg.str.toString()))

    case _ => None
  }
}

case object JwtUpickle extends JwtUpickleParser[JwtHeader, JwtClaim] {
  protected def parseHeader(header: String): JwtHeader = read[JwtHeader](header)
  protected def parseClaim(claim: String): JwtClaim = read[JwtClaim](claim)
}
