package pdi.jwt

case class JwtHeader(
  algorithm: Option[JwtAlgorithm] = None,
  typ: Option[String] = None,
  contentType: Option[String] = None
) {
  def toJson: String = JwtUtils.seqToJson(Seq(
    "typ" -> typ,
    "alg" -> algorithm.map(_.name),
    "cty" -> contentType
  ).collect {
    case (key, Some(value)) => (key -> value)
  })

  /** Assign the type to the header */
  def withType(typ: String): JwtHeader = this.copy(typ = Option(typ))

  /** Assign the default type `JWT` to the header */
  def withType: JwtHeader = this.withType("JWT")
}

object JwtHeader {
  def apply(algorithm: JwtAlgorithm): JwtHeader = new JwtHeader(Option(algorithm))

  def apply(algorithm: JwtAlgorithm, typ: String): JwtHeader = new JwtHeader(Option(algorithm), Option(typ))

  def apply(algorithm: JwtAlgorithm, typ: String, contentType: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType))
}
