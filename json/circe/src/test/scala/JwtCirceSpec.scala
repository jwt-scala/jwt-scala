package pdi.jwt

import java.time.Clock
import io.circe._

class JwtCirceSpec extends JwtJsonCommonSpec[Json] with CirceFixture {
  override def jwtJsonCommon(clock: Clock) = JwtCirce(clock)
}
