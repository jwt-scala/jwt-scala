package pdi.jwt

object JwtHeader {
  val DEFAULT_TYPE = "JWT"

  def apply(
    algorithm: Option[JwtAlgorithm] = None,
    typ: Option[String] = None,
    contentType: Option[String] = None,
    keyId: Option[String] = None
  ) = new JwtHeader(algorithm, typ, contentType, keyId)

  def apply(algorithm: Option[JwtAlgorithm]): JwtHeader = algorithm match {
    case Some(algo) => JwtHeader(algo)
    case _ => new JwtHeader(None, None, None, None)
  }

  def apply(algorithm: JwtAlgorithm): JwtHeader = new JwtHeader(Option(algorithm), Option(DEFAULT_TYPE), None, None)

  def apply(algorithm: JwtAlgorithm, typ: String): JwtHeader = new JwtHeader(Option(algorithm), Option(typ), None, None)

  def apply(algorithm: JwtAlgorithm, typ: String, contentType: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType), None)

  def apply(algorithm: JwtAlgorithm, typ: String, contentType: String, keyId: String): JwtHeader =
    new JwtHeader(Option(algorithm), Option(typ), Option(contentType), Option(keyId))
}

class JwtHeader(
  val algorithm: Option[JwtAlgorithm],
  val typ: Option[String],
  val contentType: Option[String],
  val keyId: Option[String]
) {
  def toJson: String = JwtUtils.hashToJson(Seq(
    "typ" -> typ,
    "alg" -> algorithm.map(_.name).orElse(Option("none")),
    "cty" -> contentType,
    "kid" -> keyId
  ).collect {
    case (key, Some(value)) => (key -> value)
  })

  /** Assign the type to the header */
  def withType(typ: String): JwtHeader = {
    JwtHeader(algorithm, Option(typ), contentType, keyId)
  }

  /** Assign the default type `JWT` to the header */
  def withType: JwtHeader = this.withType(JwtHeader.DEFAULT_TYPE)

  /** Assign a key id to the header */
  def withKeyId(keyId: String): JwtHeader = {
    JwtHeader(algorithm, typ, contentType, Option(keyId))
  }

  // equality code
  def canEqual(other: Any): Boolean = other.isInstanceOf[JwtHeader]

  override def equals(other: Any): Boolean = other match {
    case that: JwtHeader =>
      (that canEqual this) &&
        algorithm == that.algorithm &&
        typ == that.typ &&
        contentType == that.contentType &&
        keyId == that.keyId
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(algorithm, typ, contentType, keyId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
