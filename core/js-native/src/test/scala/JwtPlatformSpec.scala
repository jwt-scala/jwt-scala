package pdi.jwt

import scala.annotation.nowarn

trait JwtPlatformSpec { self: JwtSpec =>
  def battleTestEncode(d: DataEntryBase, @nowarn key: String, jwt: Jwt) = {
    assertEquals(d.tokenEmpty, jwt.encode(claim))
    assertEquals(d.tokenEmpty, jwt.encode(claimClass))
  }
}
