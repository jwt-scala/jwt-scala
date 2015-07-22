package pdi.jwt

import org.json4s.JValue

trait JwtJson4sImplicits {
  implicit class RichJwtClaim(claim: JwtClaim) {
    def toJValue(): JValue = JwtJson4s.writeClaim(claim)
  }

  implicit class RichJwtHeader(header: JwtHeader) {
    def toJValue(): JValue = JwtJson4s.writeHeader(header)
  }
}
