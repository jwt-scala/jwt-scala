package pdi.jwt

trait JsonDataEntryTrait[J] {
  def algo: JwtAlgorithm
  def header: String
  def headerClass: JwtHeader
  def header64: String
  def signature: String
  def token: String
  def tokenUnsigned: String
  def headerJson: J
}

trait JsonCommonFixture[J] extends Fixture {
  def claimJson: J
  def mapData(data: DataEntry): JsonDataEntryTrait[J]

  val dataJson = data.map(mapData)
}
