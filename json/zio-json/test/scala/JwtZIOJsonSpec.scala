package pdi.jwt

import java.time.Clock

import io.circe.*

class JwtZIOJsonSpec extends JwtJsonCommonSpec[Json] with ZIOJsonFixture {
  override def jwtJsonCommon(clock: Clock) = JwtCirce(clock)
}
