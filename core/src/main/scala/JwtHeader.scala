package pdi.jwt

object JwtHeader {
  val DEFAULT_TYPE = "JWT"

  def apply(algorithm: Option[JwtAlgorithm]): JwtHeader = algorithm match {
    case Some(algo) => JwtHeader(algo)
    case _          => new JwtHeader(None, None, None, None)
  }

  def apply(algorithm: JwtAlgorithm): JwtHeader =
    new JwtHeader(Option(algorithm), Option(DEFAULT_TYPE), None, None)

  def apply(algorithm: JwtAlgorithm, typ: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), None, None)

  def apply(algorithm: JwtAlgorithm, typ: String, contentType: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType), None)

  def apply(algorithm: JwtAlgorithm, typ: String, contentType: String, keyId: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType), Option(keyId))
}

case class JwtHeader(
    algorithm: Option[JwtAlgorithm] = None,
    typ: Option[String] = None,
    contentType: Option[String] = None,
    keyId: Option[String] = None
) {
  def toJson: String = JwtUtils.hashToJson(
    Seq(
      "typ" -> typ,
      "alg" -> algorithm.map(_.name).orElse(Option("none")),
      "cty" -> contentType,
      "kid" -> keyId
    ).collect { case (key, Some(value)) =>
      (key -> value)
    }
  )

  /** Assign the type to the header */
  def withType(typ: String): JwtHeader = copy(typ = Option(typ))

  /** Assign the default type `JWT` to the header */
  def withType: JwtHeader = this.withType(JwtHeader.DEFAULT_TYPE)

  /** Assign a key id to the header */
  def withKeyId(keyId: String): JwtHeader = copy(keyId = Option(keyId))

}
