package pdi.jwt

case class JsonDataEntry(
    algo: JwtAlgorithm,
    header: String,
    headerClass: JwtHeader,
    header64: String,
    signature: String,
    token: String,
    tokenUnsigned: String,
    tokenEmpty: String,
    headerJson: ujson.Value
) extends JsonDataEntryTrait[ujson.Value]

trait JwtUpickleFixture extends JsonCommonFixture[ujson.Value] {

  val claimJson: ujson.Value = ujson.read(claim)

  val headerEmptyJson: ujson.Value = ujson.read(headerEmpty)

  def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    algo = data.algo,
    header = data.header,
    headerClass = data.headerClass,
    header64 = data.header64,
    signature = data.signature,
    token = data.token,
    tokenUnsigned = data.tokenUnsigned,
    tokenEmpty = data.tokenEmpty,
    headerJson = ujson.read(data.header)
  )

}
