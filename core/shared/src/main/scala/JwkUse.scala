package pdi.jwt

/** The intended use of a JWK, the `use` member (RFC 7517 section 4.2). */
sealed trait JwkUse {
  def value: String
}

object JwkUse {
  case object Signature extends JwkUse { def value = "sig" }
  case object Encryption extends JwkUse { def value = "enc" }
  final class Other private (val value: String) extends JwkUse {
    override def equals(other: Any): Boolean = other match {
      case that: Other => value == that.value
      case _           => false
    }

    override def hashCode(): Int = value.hashCode()

    override def toString: String = s"Other($value)"
  }

  object Other {
    def apply(value: String): Other = new Other(value)
  }

  def fromString(value: String): JwkUse = value match {
    case "sig" => Signature
    case "enc" => Encryption
    case other => Other(other)
  }
}
