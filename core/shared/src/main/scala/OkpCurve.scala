package pdi.jwt

/** A curve usable in an `OKP` JWK (RFC 8037 section 2).
  *
  * `Ed25519` and `Ed448` are supported. Turning a key on these curves into a JDK `PublicKey`, and
  * verifying signatures with it, requires a JDK that provides the `EdDSA` `KeyFactory` (JDK 15+).
  * The other `OKP` curves, such as `X25519` and `X448`, are not supported.
  */
sealed trait OkpCurve {

  /** The JOSE `crv` value, e.g. `Ed25519`. */
  def name: String

  /** The fixed size in bytes of the public key `x`. */
  def keyLength: Int

  /** The X.509 SubjectPublicKeyInfo prefix that precedes the raw public key. */
  private[jwt] def x509Header: Array[Byte]
}

object OkpCurve {
  case object Ed25519 extends OkpCurve {
    def name = "Ed25519"
    def keyLength = 32
    private[jwt] def x509Header = hex("302a300506032b6570032100")
  }
  case object Ed448 extends OkpCurve {
    def name = "Ed448"
    def keyLength = 57
    private[jwt] def x509Header = hex("3043300506032b6571033a00")
  }

  def all(): Seq[OkpCurve] = Seq(Ed25519, Ed448)

  def fromName(name: String): Option[OkpCurve] = all().find(_.name == name)

  private def hex(value: String): Array[Byte] =
    value.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
}
