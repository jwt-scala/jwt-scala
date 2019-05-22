package pdi.jwt

import java.time.Clock
import spray.json._

class JwtSprayJsonSpec extends JwtJsonCommonSpec[JsObject] with SprayJsonFixture {
  override def jwtJsonCommon(clock: Clock) = JwtSprayJson(clock)
}
