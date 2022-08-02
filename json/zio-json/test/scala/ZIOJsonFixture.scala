package pdi.jwt

import zio.json.*

case class JsonDataEntry(
    algo: JwtAlgorithm,
    header: String,
    headerClass: JwtHeader,
    header64: String,
    signature: String,
    token: String,
    tokenUnsigned: String,
    tokenEmpty: String,
    headerJson: Json
) extends JsonDataEntryTrait[Json]

trait ZIOJsonFixture extends JsonCommonFixture[Json] {
  def parseString(value: String): Json = jawnParse(value).toOption.get

  val claimJson = parseString(claim)
  val headerEmptyJson = parseString(headerEmpty)

  def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    algo = data.algo,
    header = data.header,
    headerClass = data.headerClass,
    header64 = data.header64,
    signature = data.signature,
    token = data.token,
    tokenUnsigned = data.tokenUnsigned,
    tokenEmpty = data.tokenEmpty,
    headerJson = parseString(data.header)
  )
}
