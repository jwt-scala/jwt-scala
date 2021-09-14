package pdi.jwt

import argonaut.*

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

trait ArgonautFixture extends JsonCommonFixture[Json] {
  protected def parse(string: String): Json = Parse.parseOption(string).get

  override val claimJson: Json = parse(claim)

  override val headerEmptyJson: Json = parse(headerEmpty)

  override def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    data.algo,
    data.header,
    data.headerClass,
    data.header64,
    data.signature,
    data.token,
    data.tokenUnsigned,
    data.tokenEmpty,
    parse(data.header)
  )
}
