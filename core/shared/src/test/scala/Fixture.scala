package pdi.jwt

import java.time.{Clock, Instant, ZoneOffset}

import pdi.jwt.algorithms.{JwtECDSAAlgorithm, JwtHmacAlgorithm, JwtRSAAlgorithm}

trait DataEntryBase {
  def algo: JwtAlgorithm
  def header: String
  def headerClass: JwtHeader
  def header64: String
  def signature: String
  def token: String
  def tokenUnsigned: String
  def tokenEmpty: String
}

case class DataEntry[A <: JwtAlgorithm](
    algo: A,
    header: String,
    headerClass: JwtHeader,
    header64: String,
    signature: String,
    token: String = "",
    tokenUnsigned: String = "",
    tokenEmpty: String = ""
) extends DataEntryBase

trait ClockFixture {
  val expiration: Long = 1300819380
  val expirationMillis: Long = expiration * 1000
  val beforeExpirationMillis: Long = expirationMillis - 1
  val afterExpirationMillis: Long = expirationMillis + 1

  def fixedUTC(millis: Long): Clock = Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

  val afterExpirationClock: Clock = fixedUTC(afterExpirationMillis)

  val notBefore: Long = 1300819320
  val notBeforeMillis: Long = notBefore * 1000
  val beforeNotBeforeMillis: Long = notBeforeMillis - 1
  val afterNotBeforeMillis: Long = notBeforeMillis + 1

  val beforeNotBeforeClock: Clock = fixedUTC(beforeNotBeforeMillis)
  val afterNotBeforeClock: Clock = fixedUTC(afterNotBeforeMillis)

  val validTime: Long = (expiration + notBefore) / 2
  val validTimeMillis: Long = validTime * 1000
  val validTimeClock: Clock = fixedUTC(validTimeMillis)

  val ecCurveName = "secp521r1"
}

trait Fixture extends FixturePlatform with ClockFixture {

  val data = Seq(
    DataEntry[JwtHmacAlgorithm](
      JwtAlgorithm.HMD5,
      """{"typ":"JWT","alg":"HMD5"}""",
      JwtHeader(JwtAlgorithm.HMD5, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJITUQ1In0",
      "BVRxj65Lk3DXIug2IosRvw"
    ),
    DataEntry[JwtHmacAlgorithm](
      JwtAlgorithm.HS256,
      """{"typ":"JWT","alg":"HS256"}""",
      JwtHeader(JwtAlgorithm.HS256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9",
      "IPSERPZc5wyxrZ4Yiq7l31wFk_qaDY5YrnfLjIC0Lmc"
    ),
    DataEntry[JwtHmacAlgorithm](
      JwtAlgorithm.HS384,
      """{"typ":"JWT","alg":"HS384"}""",
      JwtHeader(JwtAlgorithm.HS384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9",
      "tCjCk4PefnNV6E_PByT5xumMVm6KAt_asxP8DXwcDnwsldVJi_Y7SfTVJzvyuGBY"
    ),
    DataEntry[JwtHmacAlgorithm](
      JwtAlgorithm.HS512,
      """{"typ":"JWT","alg":"HS512"}""",
      JwtHeader(JwtAlgorithm.HS512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9",
      "ngZsdQj8p2wvUAo8xCbJPwganGPnG5UnLkg7VrE6NgmQdV16UITjlBajZxcai_U5PjQdeN-yJtyA5kxf8O5BOQ"
    )
  ).map(setToken)

  val dataRSA = Seq(
    DataEntry[JwtRSAAlgorithm](
      JwtAlgorithm.RS256,
      """{"typ":"JWT","alg":"RS256"}""",
      JwtHeader(JwtAlgorithm.RS256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
      "h943pccw-3rOGJW_RlURooRk7SawDZcQiyP9iziq_LUKHtZME_UMGATeVpoc1aoGK0SlWPlVgV1HaB9fNEyziRYPi7i2K_l9XysRHhdo-luCL_D2rNK4Kv_034knQdC_pZPQ4vMviLDqHVL7w0edG-5-96fzFiP3jwV7FIz7r86fvtNgmKw8cH-cSZfEbj_vgWXT_bE_MHcCE0g4UBiXvTUbd9FpkiTugM6Lr9SXLiFKUtAraOxaKKeZ0VSLMTATK8M2PqLq4I0NnJMaZpcIp1pP9DFz07GomTpMP49Ag4CGzutFIUXz-J277OYDrLjfIT7jDnQIYuzrwE3vatwp2g"
    ),
    DataEntry[JwtRSAAlgorithm](
      JwtAlgorithm.RS384,
      """{"typ":"JWT","alg":"RS384"}""",
      JwtHeader(JwtAlgorithm.RS384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzM4NCJ9",
      "jpusk3t1NPdT7VaZLB6mO3_L4R59gSbgRM866HVZzN6qkH3vYy9y91eMs6YQZLXgg1nBi1ZY8pb4R9G_on4Xsenh-K7odRCHX-XzVbzAtnljMMChdqKp7zTAlAWF03ZrFyv91kxAQeyQSkwxDP4vP70SCLtt3_kevAzon5fE1L1DD1TNySe52TDCofd2RUPFhWzsfdAPvo_Qj1s_zG-DThHSMXXMY9GOtugyJjbDCDrl8uGeF_0XQm-wBuYQ_EGw0S9TsoI_8dggmeEyv8XwT2XKB20fKOc298GNWJ6q6E01hI0EjmWKXEtTyLG0edAF-QrNkXtkz-yX9WJmjmyVfA"
    ),
    DataEntry[JwtRSAAlgorithm](
      JwtAlgorithm.RS512,
      """{"typ":"JWT","alg":"RS512"}""",
      JwtHeader(JwtAlgorithm.RS512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9",
      "UJLq3LjgGgxQHpIHYMc48mCW1GrqwNT4sVF7IpyT6Vtbk0b_TcZ-MoWzkdYnP4-V0D8fl7kxJlLooXDWVso25UQMC66t35pAjFsQHvz7WGn1MQf5F2IOVeS2T_Qg0ckfhykw-jqXgOCrtgI-8lq_A0W8lATLoWjaQSosZxH7oYk6XJY3v5gi3reurAsrbqRCi6Gc87MdB_Yl29acAMr2_G3hun6h_VJckemOsBudLf8kGj_3lCSCY8TLncJYTLB9ZAtWhS92LpKRwPGS2CED2sQcHbq4BK10yJh-YrLrUnhCibBNMVWt1EyFf2obqSl-4Qllv4_WRnCOE4HLrosIYQ"
    )
  ).map(setToken)

  val dataECDSA = Seq(
    DataEntry[JwtECDSAAlgorithm](
      JwtAlgorithm.ES256,
      """{"typ":"JWT","alg":"ES256"}""",
      JwtHeader(JwtAlgorithm.ES256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9",
      "MIGIAkIBFmPwOO2eBdtkCko3pjjJs5Wpdi2GBhRywwptlosRQVlQxmT95uOoKE9BUjqVdyjd8o9TcNHHqM6ayPmQml0aTYICQgGDYkPc5EUfJ1F9VFvbPW1bIpX_sZ3XwyXIeL_4jt7BeKmB_LPorgeO-agmx4UdqMyCG1-Y31m8cJEPNm7h5x5V-Q"
    ),
    DataEntry[JwtECDSAAlgorithm](
      JwtAlgorithm.ES384,
      """{"typ":"JWT","alg":"ES384"}""",
      JwtHeader(JwtAlgorithm.ES384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9",
      "MEUCIQC5trx72Z6QKUKxoK_DIX9S3X5QOJBu9tC3f5i6C_1gRQIgOYnA7NoLI3CNVLbibqAwQHSyU44f-yLYGn0YaJvReMA"
    ),
    DataEntry[JwtECDSAAlgorithm](
      JwtAlgorithm.ES512,
      """{"typ":"JWT","alg":"ES512"}""",
      JwtHeader(JwtAlgorithm.ES512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzUxMiJ9",
      "MEUCICcluU9j5N40Mcr_Mo5_r5KVexcgrXH0LMVC_k1EPswPAiEA-8W2vz2bVZCzPv-S6CNDlbxNktEkOtTAg0XXiZ0ghLk"
    )
  ).map(setToken)

  val dataEdDSA = Seq(
    DataEntry(
      JwtAlgorithm.EdDSA,
      """{"typ":"JWT","alg":"EdDSA"}""",
      JwtHeader(JwtAlgorithm.EdDSA, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9",
      "I4phqhsuywTyv0Fb12v0X-ILw8tFdDlDExRTsBUYMB2yjo340KXC8L_QfUyO7-8NoMzO5k4rHPkxq8cC2xu8CQ"
    )
  ).map(setToken)

  def setToken[A <: JwtAlgorithm](entry: DataEntry[A]): DataEntry[A] = {
    entry.copy(
      token = Seq(entry.header64, claim64, entry.signature).mkString("."),
      tokenUnsigned = Seq(entry.header64, claim64, "").mkString("."),
      tokenEmpty = Seq(header64Empty, claim64, "").mkString(".")
    )
  }
}
