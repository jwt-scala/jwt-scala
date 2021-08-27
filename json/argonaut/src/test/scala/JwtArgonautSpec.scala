package pdi.jwt

import java.time.Clock

import argonaut.Json

class JwtArgonautSpec extends JwtJsonCommonSpec[Json] with ArgonautFixture {
  override def jwtJsonCommon(clock: Clock): JwtJsonCommon[Json, JwtHeader, JwtClaim] = JwtArgonaut(
    clock
  )
}
