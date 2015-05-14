package pdi.jwt

import org.json4s._
import org.json4s.JsonDSL.WithBigDecimal._
import org.json4s.jackson.JsonMethods._

trait Json4sJacksonFixture extends Json4sCommonFixture {
  def parseString(value: String): JValue = parse(value)
}
