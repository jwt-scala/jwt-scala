package pdi.jwt

import upickle.Js
import upickle.default._

class JwtUpickleSpec extends JwtJsonCommonSpec[Js.Value] with JwtUpickleFixture {
  val jwtJsonCommon = JwtUpickle
}
