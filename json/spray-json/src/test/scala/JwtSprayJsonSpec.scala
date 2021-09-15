package pdi.jwt

import java.time.Clock

import spray.json.*

class JwtSprayJsonSpec extends JwtJsonCommonSpec[JsObject] with SprayJsonFixture {
  override def jwtJsonCommon(clock: Clock) = JwtSprayJson(clock)

  // It's unfortunate but Spray Json is not reliable on sorting JSON keys when
  // stringifying. The only way would be to use `value.sortedPrint` rather than
  // `value.compactPrint` but then, you're not testing the real code anymore.
  // Let's be honest and just disable those tests.
  override def testEncoding = false
}
