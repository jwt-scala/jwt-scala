package pdi.jwt

import org.json4s._

class JwtJson4sNativeSpec extends JwtJsonCommonSpec[JObject] with Json4sNativeFixture {
  val jwtJsonCommon = JwtJson4s
}
