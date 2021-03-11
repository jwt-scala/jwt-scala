package pdi.jwt

import argonaut._

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

  override val claimJson: Json = parse(claim) match {
    case json: Json => json
    case null       => throw new RuntimeException("I want an argonaut json!")
  }

  override val headerEmptyJson: Json = parse(headerEmpty) match {
    case json: Json => json
    case null       => throw new RuntimeException("I want an argonaut json!")
  }

  override def mapData(data: DataEntryBase): JsonDataEntry = JsonDataEntry(
    data.algo,
    data.header,
    data.headerClass,
    data.header64,
    data.signature,
    data.token,
    data.tokenUnsigned,
    data.tokenEmpty,
    parse(data.header) match {
      case json: Json => json
      case null       => throw new RuntimeException("I want an argonaut json!")
    }
  )
}
