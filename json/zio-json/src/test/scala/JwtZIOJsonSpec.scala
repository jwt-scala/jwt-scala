package pdi.jwt

import java.time.Clock

import zio.json.ast.Json

class JwtZIOJsonSpec extends JwtJsonCommonSpec[Json] with ZIOJsonFixture {
  override def jwtJsonCommon(clock: Clock) = JwtZIOJson(clock)
}
