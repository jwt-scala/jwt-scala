package pdi.jwt

case class JwtOptions(
  signature: Boolean = true,
  expiration: Boolean = true,
  notBefore: Boolean = true
)

object JwtOptions {
  val DEFAULT = new JwtOptions()
}
