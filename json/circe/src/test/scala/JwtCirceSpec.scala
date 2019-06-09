package pdi.jwt

import java.time.Clock
import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._

class JwtCirceSpec extends JwtJsonCommonSpec[Json] with CirceFixture {
  override def jwtJsonCommon(clock: Clock) = JwtCirce(clock)
}
