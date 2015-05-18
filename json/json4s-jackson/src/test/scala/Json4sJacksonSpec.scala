package pdi.jwt

import org.json4s._

class JwtJson4sJacksonSpec extends JwtJsonCommonSpec[JObject] with Json4sJacksonFixture {
  val jwtJsonCommon = JwtJson4s
}
