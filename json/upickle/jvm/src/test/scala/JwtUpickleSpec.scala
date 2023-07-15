package pdi.jwt

import java.time.Clock

class JwtUpickleSpec extends JwtJsonCommonSpec[ujson.Value] with JwtUpickleFixture {
  def jwtJsonCommon(clock: Clock) = JwtUpickle(clock)
}
