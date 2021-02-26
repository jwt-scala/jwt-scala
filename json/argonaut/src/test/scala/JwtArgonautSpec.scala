package pdi.jwt

import argonaut.Json
import java.time.Clock

class JwtArgonautSpec extends JwtJsonCommonSpec[Json] with ArgonautFixture {
  override def jwtJsonCommon(clock: Clock): JwtJsonCommon[Json, JwtHeader, JwtClaim] = JwtArgonaut(
    clock
  )
}
