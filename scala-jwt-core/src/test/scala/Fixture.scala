package pdi.jwt

import mockit.MockUp
import mockit.Mock
import java.time.Instant

case class DataEntry(
  algo: String,
  header: String,
  headerClass: JwtHeader,
  header64: String,
  signature: String,
  token: String = "",
  tokenUnsigned: String = ""
)

trait Fixture {
  val secretKey = Option("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")

  val expiration: Long = 1300819380
  val expirationMillis: Long = expiration * 1000
  val beforeExpirationMillis: Long = expirationMillis - 1
  val afterExpirationMillis: Long = expirationMillis + 1

  def mockInstant(now: Long) = {
    new MockUp[Instant]() {
      @Mock
      def toEpochMilli: Long = now
    }
  }

  def mockBeforeExpiration = mockInstant(beforeExpirationMillis)
  def mockAfterExpiration = mockInstant(afterExpirationMillis)

  val claim = s"""{"iss":"joe","exp":${expiration},"http://example.com/is_root":true}"""
  val claimClass = JwtClaim("""{"http://example.com/is_root":true}""", issuer = Option("joe"), expiration = Option(expiration))
  val claim64 = "eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ=="

  val data = Seq(
    DataEntry (
      "HmacMD5",
      """{"typ":"JWT","alg":"HmacMD5"}""",
      JwtHeader("HmacMD5", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjTUQ1In0=",
      "eW2omY9kpadPg_M5ECg_SQ=="
    ),

    DataEntry (
      "HMD5",
      """{"typ":"JWT","alg":"HMD5"}""",
      JwtHeader("HMD5", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJITUQ1In0=",
      "DN76C8aGlSM-Rwy75HHnFA=="
    ),

    DataEntry (
      "HmacSHA1",
      """{"typ":"JWT","alg":"HmacSHA1"}""",
      JwtHeader("HmacSHA1", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMSJ9",
      "FbaxYjZdyrsZcfzJNqgD0DrXP8g="
    ),

    DataEntry (
      "HS1",
      """{"typ":"JWT","alg":"HS1"}""",
      JwtHeader("HS1", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzEifQ==",
      "ehprddUi3mH2cXVN35pGNHkeSBk="
    ),

    DataEntry (
      "HmacSHA256",
      """{"typ":"JWT","alg":"HmacSHA256"}""",
      JwtHeader("HmacSHA256", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMjU2In0=",
      "8dFiIcid4qZ5zHjfRNpIbyrc5GWQbddz5UZVkqzvQKU="
    ),

    DataEntry (
      "HS256",
      """{"typ":"JWT","alg":"HS256"}""",
      JwtHeader("HS256", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9",
      "hZOTRuD0YLWUYyhhDp0KXfFCtgAxiTzfwyFXLuPldEk="
    ),

    DataEntry (
      "HmacSHA512",
      """{"typ":"JWT","alg":"HmacSHA512"}""",
      JwtHeader("HmacSHA512", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBNTEyIn0=",
      "DRtzJb01Gk1tHPxgiK13gAnsMev4hF6Otr7oNI7oJp0YNrIkYdnz-ikf3OhwSNWdvpHslv0oH5jEsi8LwKkg7A=="
    ),

    DataEntry (
      "HS512",
      """{"typ":"JWT","alg":"HS512"}""",
      JwtHeader("HS512", "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9",
      "3M6Q4hImCsJj-c7mnN6NFnmnoDSv5n1UF8WUNFQ_pvFaRYGNKiHDg_ZJhUYapTGzp6w-0fsXfp6WVHHGy--MJA=="
    )
  ).map { d =>
    d.copy(
      token = Seq(d.header64, claim64, d.signature).mkString("."),
      tokenUnsigned = Seq(d.header64, claim64, "").mkString(".")
    )
  }
}
