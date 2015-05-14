package pdi.jwt

import play.api.libs.json.JsObject

case class JsonDataEntry (
  algo: JwtAlgorithm,
  header: String,
  headerClass: JwtHeader,
  header64: String,
  signature: String,
  token: String,
  tokenUnsigned: String,
  headerJson: JsObject) extends JsonDataEntryTrait[JsObject]

trait JsonFixture extends JsonCommonFixture[JsObject] {
  val claimJson = jwtClaimWriter.writes(claimClass).as[JsObject]

  def mapData(data: DataEntry): JsonDataEntry = JsonDataEntry(
    algo = data.algo,
    header = data.header,
    headerClass = data.headerClass,
    header64 = data.header64,
    signature = data.signature,
    token = data.token,
    tokenUnsigned = data.tokenUnsigned,
    headerJson = jwtHeaderWriter.writes(data.headerClass).as[JsObject]
  )
}
