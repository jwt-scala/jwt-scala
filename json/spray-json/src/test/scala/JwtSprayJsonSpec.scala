package pdi.jwt

import spray.json._

class JwtSprayJsonSpec extends JwtJsonCommonSpec[JsObject] with SprayJsonFixture {
  val jwtJsonCommon = JwtSprayJson
}
