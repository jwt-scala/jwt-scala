package pdi.jwt

/** The intended use of a JWK, the `use` member (RFC 7517 section 4.2). */
sealed trait JwkUse {
  def value: String
}

object JwkUse {
  case object Signature extends JwkUse { def value = "sig" }
  case object Encryption extends JwkUse { def value = "enc" }
  final case class Other(value: String) extends JwkUse

  def fromString(value: String): JwkUse = value match {
    case "sig" => Signature
    case "enc" => Encryption
    case other => Other(other)
  }
}
