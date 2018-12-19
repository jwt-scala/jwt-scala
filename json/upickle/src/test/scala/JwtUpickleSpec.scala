package pdi.jwt

class JwtUpickleSpec extends JwtJsonCommonSpec[ujson.Value] with JwtUpickleFixture {
  val jwtJsonCommon = JwtUpickle
}
