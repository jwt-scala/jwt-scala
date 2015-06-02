package pdi.jwt

import org.json4s._
import org.json4s.JsonDSL.WithBigDecimal._

case class JsonDataEntry (
  algo: JwtAlgorithm,
  header: String,
  headerClass: JwtHeader,
  header64: String,
  signature: String,
  token: String,
  tokenUnsigned: String,
  tokenEmpty: String,
  headerJson: JObject) extends JsonDataEntryTrait[JObject]

trait Json4sCommonFixture extends JsonCommonFixture[JObject] {
  def parseString(value: String): JValue

  val claimJson = parseString(claim) match {
    case j: JObject => j
    case _ => throw new RuntimeException("I want a JObject!")
  }

  val headerEmptyJson = parseString(headerEmpty) match {
    case j: JObject => j
    case _ => throw new RuntimeException("I want a JObject!")
  }

  def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    algo = data.algo,
    header = data.header,
    headerClass = data.headerClass,
    header64 = data.header64,
    signature = data.signature,
    token = data.token,
    tokenUnsigned = data.tokenUnsigned,
    tokenEmpty = data.tokenEmpty,
    headerJson = parseString(data.header) match {
      case j: JObject => j
      case _ => throw new RuntimeException("I want a JObject!")
    }
  )
}
