package pdi.jwt

import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._
import cats.data.Xor

class JwtCirceSpec extends JwtJsonCommonSpec[Json] with CirceFixture {
  val jwtJsonCommon = JwtCirce
}
