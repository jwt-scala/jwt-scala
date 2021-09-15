package pdi.jwt

import org.json4s.*
import org.json4s.jackson.JsonMethods.*

trait Json4sJacksonFixture extends Json4sCommonFixture {
  def parseString(value: String): JValue = parse(value)
}
