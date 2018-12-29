package pdi.jwt

import argonaut.Json

class JwtArgonautSpec extends JwtJsonCommonSpec[Json] with ArgonautFixture {
 override val jwtJsonCommon: JwtJsonCommon[Json, JwtHeader, JwtClaim] = JwtArgonaut
}
