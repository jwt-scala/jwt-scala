package pdi.jwt

import io.circe._
import io.circe.jawn.{parse => jawnParse}

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

trait CirceFixture extends JsonCommonFixture[Json] {
  def parseString(value: String): Json = jawnParse(value).toOption.get

  val claimJson = parseString(claim) match {
    case j: Json => j
    case null    => throw new RuntimeException("I want a Circe Json!")
  }

  val headerEmptyJson = parseString(headerEmpty) match {
    case j: Json => j
    case null    => throw new RuntimeException("I want a Circe Json!")
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
      case j: Json => j
      case null    => throw new RuntimeException("I want a Circe Json!")
    }
  )
}
