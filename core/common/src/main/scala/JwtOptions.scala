package pdi.jwt

case class JwtOptions(
  signature: Boolean = true,
  expiration: Boolean = true,
  notBefore: Boolean = true,
  leeway: Long = 0 // in seconds
)

object JwtOptions {
  val DEFAULT = new JwtOptions()
}
