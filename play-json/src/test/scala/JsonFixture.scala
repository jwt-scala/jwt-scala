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
  headerJson: JsObject)

trait JsonFixture extends Fixture {
  val claimJson = jwtClaimWriter.writes(claimClass).as[JsObject]

  val dataJson = data.map { d =>
    JsonDataEntry(
      algo = d.algo,
      header = d.header,
      headerClass = d.headerClass,
      header64 = d.header64,
      signature = d.signature,
      token = d.token,
      tokenUnsigned = d.tokenUnsigned,
      headerJson = jwtHeaderWriter.writes(d.headerClass).as[JsObject]
    )
  }
}
