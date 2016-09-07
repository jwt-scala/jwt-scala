package pdi.jwt

import upickle.Js
import upickle.json
import upickle.default._

case class JsonDataEntry(
  algo: JwtAlgorithm,
  header: String,
  headerClass: JwtHeader,
  header64: String,
  signature: String,
  token: String,
  tokenUnsigned: String,
  tokenEmpty: String,
  headerJson: Js.Value) extends JsonDataEntryTrait[Js.Value]


trait UpickleFixture extends JsonCommonFixture[Js.Value] {

  val claimJson: Js.Value = json.read(claim)

  val headerEmptyJson: Js.Value = json.read(headerEmpty)

  def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    algo = data.algo,
    header = data.header,
    headerClass = data.headerClass,
    header64 = data.header64,
    signature = data.signature,
    token = data.token,
    tokenUnsigned = data.tokenUnsigned,
    tokenEmpty = data.tokenEmpty,
    headerJson = json.read(data.header)
  )

}
