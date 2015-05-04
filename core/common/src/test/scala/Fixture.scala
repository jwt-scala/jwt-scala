package pdi.jwt

case class DataEntry(
  algo: String,
  header: String,
  headerClass: JwtHeader,
  header64: String,
  signature: String,
  token: String = "",
  tokenUnsigned: String = ""
)

trait Fixture extends TimeFixture {
  val secretKey = Option("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")

  val expiration: Long = 1300819380
  val expirationMillis: Long = expiration * 1000
  val beforeExpirationMillis: Long = expirationMillis - 1
  val afterExpirationMillis: Long = expirationMillis + 1

  def mockBeforeExpiration = mockTime(beforeExpirationMillis)
  def mockAfterExpiration = mockTime(afterExpirationMillis)

  val claim = s"""{"iss":"joe","exp":${expiration},"http://example.com/is_root":true}"""
  val claimClass = JwtClaim("""{"http://example.com/is_root":true}""", issuer = Option("joe"), expiration = Option(expiration))
  val claim64 = "eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"

  val data = Seq(
    DataEntry (
      "HmacMD5",
      """{"typ":"JWT","alg":"HmacMD5"}""",
      JwtHeader("HmacMD5", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjTUQ1In0",
      "de-uQNyKSx8AskxaptmHpg"
    ),

    DataEntry (
      "HMD5",
      """{"typ":"JWT","alg":"HMD5"}""",
      JwtHeader("HMD5", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJITUQ1In0",
      "BVRxj65Lk3DXIug2IosRvw"
    ),

    DataEntry (
      "HmacSHA1",
      """{"typ":"JWT","alg":"HmacSHA1"}""",
      JwtHeader("HmacSHA1", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMSJ9",
      "MfxeCEAj4rZmNvcrU_LVRBEkGo8"
    ),

    DataEntry (
      "HS1",
      """{"typ":"JWT","alg":"HS1"}""",
      JwtHeader("HS1", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzEifQ",
      "z9OBLoWU_-Ekk3sE2e54kn-GWzY"
    ),

    DataEntry (
      "HmacSHA256",
      """{"typ":"JWT","alg":"HmacSHA256"}""",
      JwtHeader("HmacSHA256", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMjU2In0",
      "-3BM6yrNy3a8E2QtEYszKes2Rij80sfpgBAmzrJeJuk"
    ),

    DataEntry (
      "HS256",
      """{"typ":"JWT","alg":"HS256"}""",
      JwtHeader("HS256", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9",
      "IPSERPZc5wyxrZ4Yiq7l31wFk_qaDY5YrnfLjIC0Lmc"
    ),

    DataEntry (
      "HmacSHA384",
      """{"typ":"JWT","alg":"HmacSHA384"}""",
      JwtHeader("HmacSHA384", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMzg0In0",
      "W5LuS1ga9iyoUBFcm5gxI7OGLnouEomfIoq47KTmM62Xn3C5wBWLHi8hRVrte9Uz"
    ),

    DataEntry (
      "HS384",
      """{"typ":"JWT","alg":"HS384"}""",
      JwtHeader("HS384", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9",
      "tCjCk4PefnNV6E_PByT5xumMVm6KAt_asxP8DXwcDnwsldVJi_Y7SfTVJzvyuGBY"
    ),

    DataEntry (
      "HmacSHA512",
      """{"typ":"JWT","alg":"HmacSHA512"}""",
      JwtHeader("HmacSHA512", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBNTEyIn0",
      "egt3RJEC5VOJBz5MT0lyV4p8dRJYu5FYQjNCzS_4iVT3d2W-Rzqr305ndzw-uvBzHRxQj0RQaxsnrvN8_uwbEQ"
    ),

    DataEntry (
      "HS512",
      """{"typ":"JWT","alg":"HS512"}""",
      JwtHeader("HS512", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9",
      "ngZsdQj8p2wvUAo8xCbJPwganGPnG5UnLkg7VrE6NgmQdV16UITjlBajZxcai_U5PjQdeN-yJtyA5kxf8O5BOQ"
    )
  ).map { d =>
    d.copy(
      token = Seq(d.header64, claim64, d.signature).mkString("."),
      tokenUnsigned = Seq(d.header64, claim64, "").mkString(".")
    )
  }
}
