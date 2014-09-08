package pdi.scala.jwt

case class JwtHeader(
  algorithm: Option[String] = None,
  typ: Option[String] = None,
  contentType: Option[String] = None
) {
  def toJson: String = JwtUtils.mapToJson(Map(
    "alg" -> algorithm,
    "typ" -> typ,
    "cty" -> contentType
  ).collect {
    case (key, Some(value)) => (key -> value)
  })
}
