package pdi.jwt

import java.time.Clock

import zio.json.*
import zio.json.ast.*
import zio.json.ast.JsonCursor.*

trait JwtZIOJsonParser[H, C] extends JwtJsonCommon[Json, H, C] {
  override protected def parse(value: String): Json =
    value.fromJson[Json].fold(err => throw new Exception(err), Predef.identity)

  override protected def stringify(value: Json): String = value.toJson

  override protected def getAlgorithm(header: Json): Option[JwtAlgorithm] =
    header
      .get(field("alg").isString)
      .toOption
      .map(_.value)
      .filterNot(_ == "none")
      .map(JwtAlgorithm.fromString)
}

object JwtZIOJson extends JwtZIOJson(Clock.systemUTC) {
  def apply(clock: Clock): JwtZIOJson = new JwtZIOJson(clock)

}

class JwtZIOJson(override val clock: Clock) extends JwtZIOJsonParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = parseHeaderHelp(header)

  def parseClaim(claim: String): JwtClaim = parseClaimHelp(claim)

  private def parseHeaderHelp(header: String): JwtHeader = {
    val json = parse(header)
    JwtHeader(
      algorithm = getAlgorithm(json),
      typ = json.get(field("typ").isString).toOption.map(_.value),
      contentType = json.get(field("cty").isString).toOption.map(_.value),
      keyId = json.get(field("kid").isString).toOption.map(_.value)
    )
  }

  private def parseClaimHelp(claim: String): JwtClaim = {
    val json = parse(claim)
    val keys = Set("iss", "sub", "aud", "exp", "nbf", "iat", "jti")
    val content =
      json
        .as[Json.Obj]
        .map(obj => obj.fields.filterNot{ case (key,_) => keys.contains(key)})
        .map(tuples => Json.Obj(tuples))
        .getOrElse(Json.Obj())

    val audience =
      json
        .get(field("aud"))
        .flatMap(_.as[Set[String]])
        .orElse(json.get(field("aud")).flatMap(_.as[String]).map(s => Set(s)))
        .toOption

    JwtClaim(
      content = content.toJson,
      issuer = json.get(field("iss").isString).toOption.map(_.value),
      subject = json.get(field("sub").isString).toOption.map(_.value),
      audience = audience,
      expiration = json.get(field("exp").isNumber).toOption.map(_.value.longValue()),
      notBefore = json.get(field("nbf").isNumber).toOption.map(_.value.longValue()),
      issuedAt = json.get(field("iat").isNumber).toOption.map(_.value.longValue()),
      jwtId = json.get(field("jti").isString).toOption.map(_.value)
    )
  }

}
