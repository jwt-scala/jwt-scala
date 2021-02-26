package pdi.jwt

import spray.json._

case class JsonDataEntry(
    algo: JwtAlgorithm,
    header: String,
    headerClass: JwtHeader,
    header64: String,
    signature: String,
    token: String,
    tokenUnsigned: String,
    tokenEmpty: String,
    headerJson: JsObject
) extends JsonDataEntryTrait[JsObject]

trait SprayJsonFixture extends JsonCommonFixture[JsObject] {
  def parseString(value: String): JsObject = value.parseJson.asJsObject

  val claimJson = parseString(claim) match {
    case j: JsObject => j
    case _           => throw new RuntimeException("I want a spray-json!")
  }

  val headerEmptyJson = parseString(headerEmpty) match {
    case j: JsObject => j
    case _           => throw new RuntimeException("I want a spray-json!")
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
      case j: JsObject => j
      case _           => throw new RuntimeException("I want a spray-json!")
    }
  )
}
