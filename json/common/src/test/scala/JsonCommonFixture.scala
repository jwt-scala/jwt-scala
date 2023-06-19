package pdi.jwt

trait JsonDataEntryTrait[J] extends DataEntryBase {
  def headerJson: J
}

trait JsonCommonFixture[J] extends Fixture {
  def claimJson: J
  def headerEmptyJson: J
  def mapData(data: DataEntryBase): JsonDataEntryTrait[J]

  val dataJson = data.map(mapData)
  val dataRSAJson = dataRSA.map(mapData)
  val dataECDSAJson = dataECDSA.map(mapData)
}
