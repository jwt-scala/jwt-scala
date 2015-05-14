package pdi.jwt

import play.api.libs.json.JsObject

class JwtJsonSpec extends JwtJsonCommonSpec[JsObject] with JsonFixture {
  val jwtJsonCommon = JwtJson
}
