package pdi.scala.jwt

case class JwtHeader(
  algorithm: Option[String] = None,
  typ: Option[String] = None,
  contentType: Option[String] = None
) {
  def toJson: String = JwtUtils.seqToJson(Seq(
    "typ" -> typ,
    "alg" -> algorithm,
    "cty" -> contentType
  ).collect {
    case (key, Some(value)) => (key -> value)
  })

  def withType(typ: String): JwtHeader = this.copy(typ = Option(typ))

  def withType: JwtHeader = this.withType("JWT")
}

object JwtHeader {
  def apply(algorithm: String): JwtHeader = new JwtHeader(Option(algorithm))

  def apply(algorithm: String, typ: String): JwtHeader = new JwtHeader(Option(algorithm), Option(typ))

  def apply(algorithm: String, typ: String, contentType: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType))
}
