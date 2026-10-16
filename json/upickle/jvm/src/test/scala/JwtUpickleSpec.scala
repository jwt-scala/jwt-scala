package pdi.jwt

import java.time.Clock

class JwtUpickleSpec extends JwtJsonCommonSpec[ujson.Value] with JwtUpickleFixture {
  def jwtJsonCommon(clock: Clock): JwtUpickle = JwtUpickle(clock)

  test("should read all header fields without changing the JSON object") {
    val json = ujson.Obj("alg" -> "HS256", "typ" -> "JWT", "cty" -> "json", "kid" -> "key-1")
    val before = ujson.write(json)
    val header = upickle.default.read[JwtHeader](json)(JwtUpickle.jwtUpickleHeaderReadWrite)

    assertEquals(header, JwtHeader(JwtAlgorithm.HS256, "JWT", "json", "key-1"))
    assertEquals(ujson.write(json), before)
  }
}
