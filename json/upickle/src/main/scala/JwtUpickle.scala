package pdi.jwt

import upickle.Js
import upickle.json
import upickle.default._

/**
  * Created by alonsodomin on 07/09/2016.
  */
object JwtUpickle extends JwtJsonCommon[Js.Value] {
  override protected def parse(value: String): Js.Value = json.read(value)

  override protected def stringify(value: Js.Value): String = json.write(value)

  override protected def getAlgorithm(header: Js.Value): Option[JwtAlgorithm] = header match {
    case obj: Js.Obj =>
      val fields = obj.toMap
      fields.get("alg").flatMap(alg => JwtAlgorithm.optionFromString(alg.str))

    case _ => None
  }

  // Abstract methods
  override protected def parseHeader(header: String): JwtHeader = read[JwtHeader](header)

  override protected def parseClaim(claim: String): JwtClaim = read[JwtClaim](claim)

}
