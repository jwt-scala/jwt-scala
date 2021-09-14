package pdi.jwt

import org.json4s.*
import org.json4s.native.JsonMethods.*

trait Json4sNativeFixture extends Json4sCommonFixture {
  def parseString(value: String): JValue = parse(value)
}
