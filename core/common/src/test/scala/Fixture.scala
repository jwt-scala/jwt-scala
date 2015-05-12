package pdi.jwt

case class DataEntry(
  algo: JwtAlgorithm,
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

  val publicKeyRSA = Option("""-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvzoCEC2rpSpJQaWZbUml
sDNwp83Jr4fi6KmBWIwnj1MZ6CUQ7rBasuLI8AcfX5/10scSfQNCsTLV2tMKQaHu
vyrVfwY0dINk+nkqB74QcT2oCCH9XduJjDuwWA4xLqAKuF96FsIes52opEM50W7/
W7DZCKXkC8fFPFj6QF5ZzApDw2Qsu3yMRmr7/W9uWeaTwfPx24YdY7Ah+fdLy3KN
40vXv9c4xiSafVvnx9BwYL7H1Q8NiK9LGEN6+JSWfgckQCs6UUBOXSZdreNN9zbQ
Cwyzee7bOJqXUDAuLcFARzPw1EsZAyjVtGCKIQ0/btqK+jFunT2NBC8RItanDZpp
tQIDAQAB
-----END PUBLIC KEY-----""")

  val privateKeyRSA = Option("""-----BEGIN RSA PRIVATE KEY-----
MIIEpQIBAAKCAQEAvzoCEC2rpSpJQaWZbUmlsDNwp83Jr4fi6KmBWIwnj1MZ6CUQ
7rBasuLI8AcfX5/10scSfQNCsTLV2tMKQaHuvyrVfwY0dINk+nkqB74QcT2oCCH9
XduJjDuwWA4xLqAKuF96FsIes52opEM50W7/W7DZCKXkC8fFPFj6QF5ZzApDw2Qs
u3yMRmr7/W9uWeaTwfPx24YdY7Ah+fdLy3KN40vXv9c4xiSafVvnx9BwYL7H1Q8N
iK9LGEN6+JSWfgckQCs6UUBOXSZdreNN9zbQCwyzee7bOJqXUDAuLcFARzPw1EsZ
AyjVtGCKIQ0/btqK+jFunT2NBC8RItanDZpptQIDAQABAoIBAQCsssO4Pra8hFMC
gX7tr0x+tAYy1ewmpW8stiDFilYT33YPLKJ9HjHbSms0MwqHftwwTm8JDc/GXmW6
qUui+I64gQOtIzpuW1fvyUtHEMSisI83QRMkF6fCSQm6jJ6oQAtOdZO6R/gYOPNb
3gayeS8PbMilQcSRSwp6tNTVGyC33p43uUUKAKHnpvAwUSc61aVOtw2wkD062XzM
hJjYpHm65i4V31AzXo8HF42NrAtZ8K/AuQZne5F/6F4QFVlMKzUoHkSUnTp60XZx
X77GuyDeDmCgSc2J7xvR5o6VpjsHMo3ek0gJk5ZBnTgkHvnpbULCRxTmDfjeVPue
v3NN2TBFAoGBAPxbqNEsXPOckGTvG3tUOAAkrK1hfW3TwvrW/7YXg1/6aNV4sklc
vqn/40kCK0v9xJIv9FM/l0Nq+CMWcrb4sjLeGwHAa8ASfk6hKHbeiTFamA6FBkvQ
//7GP5khD+y62RlWi9PmwJY21lEkn2mP99THxqvZjQiAVNiqlYdwiIc7AoGBAMH8
f2Ay7Egc2KYRYU2qwa5E/Cljn/9sdvUnWM+gOzUXpc5sBi+/SUUQT8y/rY4AUVW6
YaK7chG9YokZQq7ZwTCsYxTfxHK2pnG/tXjOxLFQKBwppQfJcFSRLbw0lMbQoZBk
S+zb0ufZzxc2fJfXE+XeJxmKs0TS9ltQuJiSqCPPAoGBALEc84K7DBG+FGmCl1sb
ZKJVGwwknA90zCeYtadrIT0/VkxchWSPvxE5Ep+u8gxHcqrXFTdILjWW4chefOyF
5ytkTrgQAI+xawxsdyXWUZtd5dJq8lxLtx9srD4gwjh3et8ZqtFx5kCHBCu29Fr2
PA4OmBUMfrs0tlfKgV+pT2j5AoGBAKnA0Z5XMZlxVM0OTH3wvYhI6fk2Kx8TxY2G
nxsh9m3hgcD/mvJRjEaZnZto6PFoqcRBU4taSNnpRr7+kfH8sCht0k7D+l8AIutL
ffx3xHv9zvvGHZqQ1nHKkaEuyjqo+5kli6N8QjWNzsFbdvBQ0CLJoqGhVHsXuWnz
W3Z4cBbVAoGAEtnwY1OJM7+R2u1CW0tTjqDlYU2hUNa9t1AbhyGdI2arYp+p+umA
b5VoYLNsdvZhqjVFTrYNEuhTJFYCF7jAiZLYvYm0C99BqcJnJPl7JjWynoNHNKw3
9f6PIOE1rAmPE8Cfz/GFF5115ZKVlq+2BY8EKNxbCIy2d/vMEvisnXI=
-----END RSA PRIVATE KEY-----""")

  val publicKeyEC = Option("""-----BEGIN PUBLIC KEY-----
MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEFOep1TN1rATCCgVTjOmtD9jT2vjyIMlk
DvyorUIqCT5c7c9nQsgy2RFgA1OeNpTGxRmptztVOH02RZm3PMeWdA==
-----END PUBLIC KEY-----""")

  val privateKeyEC = Option("""-----BEGIN EC PRIVATE KEY-----
MHQCAQEEIPDiahgVmw2V91tqGnwGV5I25bdKByFj7hoojmyN+LYYoAcGBSuBBAAK
oUQDQgAEFOep1TN1rATCCgVTjOmtD9jT2vjyIMlkDvyorUIqCT5c7c9nQsgy2RFg
A1OeNpTGxRmptztVOH02RZm3PMeWdA==
-----END EC PRIVATE KEY-----
""")

  def setToken(entry: DataEntry): DataEntry = {
    entry.copy(
      token = Seq(entry.header64, claim64, entry.signature).mkString("."),
      tokenUnsigned = Seq(entry.header64, claim64, "").mkString(".")
    )
  }

  val data = Seq(
    DataEntry (
      JwtAlgorithm.HmacMD5,
      """{"typ":"JWT","alg":"HmacMD5"}""",
      JwtHeader(JwtAlgorithm.HmacMD5, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjTUQ1In0",
      "de-uQNyKSx8AskxaptmHpg"
    ),

    DataEntry (
      JwtAlgorithm.HMD5,
      """{"typ":"JWT","alg":"HMD5"}""",
      JwtHeader(JwtAlgorithm.HMD5, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJITUQ1In0",
      "BVRxj65Lk3DXIug2IosRvw"
    ),

    DataEntry (
      JwtAlgorithm.HmacSHA1,
      """{"typ":"JWT","alg":"HmacSHA1"}""",
      JwtHeader(JwtAlgorithm.HmacSHA1, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMSJ9",
      "MfxeCEAj4rZmNvcrU_LVRBEkGo8"
    ),

    DataEntry (
      JwtAlgorithm.HS1,
      """{"typ":"JWT","alg":"HS1"}""",
      JwtHeader(JwtAlgorithm.HS1, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzEifQ",
      "z9OBLoWU_-Ekk3sE2e54kn-GWzY"
    ),

    DataEntry (
      JwtAlgorithm.HmacSHA256,
      """{"typ":"JWT","alg":"HmacSHA256"}""",
      JwtHeader(JwtAlgorithm.HmacSHA256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMjU2In0",
      "-3BM6yrNy3a8E2QtEYszKes2Rij80sfpgBAmzrJeJuk"
    ),

    DataEntry (
      JwtAlgorithm.HS256,
      """{"typ":"JWT","alg":"HS256"}""",
      JwtHeader(JwtAlgorithm.HS256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9",
      "IPSERPZc5wyxrZ4Yiq7l31wFk_qaDY5YrnfLjIC0Lmc"
    ),

    DataEntry (
      JwtAlgorithm.HmacSHA384,
      """{"typ":"JWT","alg":"HmacSHA384"}""",
      JwtHeader(JwtAlgorithm.HmacSHA384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMzg0In0",
      "W5LuS1ga9iyoUBFcm5gxI7OGLnouEomfIoq47KTmM62Xn3C5wBWLHi8hRVrte9Uz"
    ),

    DataEntry (
      JwtAlgorithm.HS384,
      """{"typ":"JWT","alg":"HS384"}""",
      JwtHeader(JwtAlgorithm.HS384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzM4NCJ9",
      "tCjCk4PefnNV6E_PByT5xumMVm6KAt_asxP8DXwcDnwsldVJi_Y7SfTVJzvyuGBY"
    ),

    DataEntry (
      JwtAlgorithm.HmacSHA512,
      """{"typ":"JWT","alg":"HmacSHA512"}""",
      JwtHeader(JwtAlgorithm.HmacSHA512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBNTEyIn0",
      "egt3RJEC5VOJBz5MT0lyV4p8dRJYu5FYQjNCzS_4iVT3d2W-Rzqr305ndzw-uvBzHRxQj0RQaxsnrvN8_uwbEQ"
    ),

    DataEntry (
      JwtAlgorithm.HS512,
      """{"typ":"JWT","alg":"HS512"}""",
      JwtHeader(JwtAlgorithm.HS512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9",
      "ngZsdQj8p2wvUAo8xCbJPwganGPnG5UnLkg7VrE6NgmQdV16UITjlBajZxcai_U5PjQdeN-yJtyA5kxf8O5BOQ"
    )
  ).map(setToken)

  val dataRSA = Seq(
    DataEntry (
      JwtAlgorithm.RS256,
      """{"typ":"JWT","alg":"RS256"}""",
      JwtHeader(JwtAlgorithm.RS256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9",
        "h943pccw-3rOGJW_RlURooRk7SawDZcQiyP9iziq_LUKHtZME_UMGATeVpoc1aoGK0SlWPlVgV1HaB9fNEyziRYPi7i2K_l9XysRHhdo-luCL_D2rNK4Kv_034knQdC_pZPQ4vMviLDqHVL7w0edG-5-96fzFiP3jwV7FIz7r86fvtNgmKw8cH-cSZfEbj_vgWXT_bE_MHcCE0g4UBiXvTUbd9FpkiTugM6Lr9SXLiFKUtAraOxaKKeZ0VSLMTATK8M2PqLq4I0NnJMaZpcIp1pP9DFz07GomTpMP49Ag4CGzutFIUXz-J277OYDrLjfIT7jDnQIYuzrwE3vatwp2g"
    ),

    DataEntry (
      JwtAlgorithm.RS384,
      """{"typ":"JWT","alg":"RS384"}""",
      JwtHeader(JwtAlgorithm.RS384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzM4NCJ9",
        "jpusk3t1NPdT7VaZLB6mO3_L4R59gSbgRM866HVZzN6qkH3vYy9y91eMs6YQZLXgg1nBi1ZY8pb4R9G_on4Xsenh-K7odRCHX-XzVbzAtnljMMChdqKp7zTAlAWF03ZrFyv91kxAQeyQSkwxDP4vP70SCLtt3_kevAzon5fE1L1DD1TNySe52TDCofd2RUPFhWzsfdAPvo_Qj1s_zG-DThHSMXXMY9GOtugyJjbDCDrl8uGeF_0XQm-wBuYQ_EGw0S9TsoI_8dggmeEyv8XwT2XKB20fKOc298GNWJ6q6E01hI0EjmWKXEtTyLG0edAF-QrNkXtkz-yX9WJmjmyVfA"
    ),

    DataEntry (
      JwtAlgorithm.RS512,
      """{"typ":"JWT","alg":"RS512"}""",
      JwtHeader(JwtAlgorithm.RS512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9",
        "UJLq3LjgGgxQHpIHYMc48mCW1GrqwNT4sVF7IpyT6Vtbk0b_TcZ-MoWzkdYnP4-V0D8fl7kxJlLooXDWVso25UQMC66t35pAjFsQHvz7WGn1MQf5F2IOVeS2T_Qg0ckfhykw-jqXgOCrtgI-8lq_A0W8lATLoWjaQSosZxH7oYk6XJY3v5gi3reurAsrbqRCi6Gc87MdB_Yl29acAMr2_G3hun6h_VJckemOsBudLf8kGj_3lCSCY8TLncJYTLB9ZAtWhS92LpKRwPGS2CED2sQcHbq4BK10yJh-YrLrUnhCibBNMVWt1EyFf2obqSl-4Qllv4_WRnCOE4HLrosIYQ"
    )
  ).map(setToken)

  val dataECDSA = Seq(
    DataEntry (
      JwtAlgorithm.ES256,
      """{"typ":"JWT","alg":"ES256"}""",
      JwtHeader(JwtAlgorithm.ES256, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9",
      "MEYCIQC5Gr7WWeNsJ_9P8qVxuSFCwLcDhuKDBGRR7kooMgENxgIhAPF1JxcN5SQD_6jPbRyiSjkHJj_e0GnkHecgRlxOOOKg"
    ) //,

    /*DataEntry (
      JwtAlgorithm.ES384,
      """{"typ":"JWT","alg":"ES384"}""",
      JwtHeader(JwtAlgorithm.ES384, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzM4NCJ9",
      "MEUCIQC5trx72Z6QKUKxoK_DIX9S3X5QOJBu9tC3f5i6C_1gRQIgOYnA7NoLI3CNVLbibqAwQHSyU44f-yLYGn0YaJvReMA"
    ),

    DataEntry (
      JwtAlgorithm.ES512,
      """{"typ":"JWT","alg":"ES512"}""",
      JwtHeader(JwtAlgorithm.ES512, "JWT"),
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzUxMiJ9",
      "MEUCICcluU9j5N40Mcr_Mo5_r5KVexcgrXH0LMVC_k1EPswPAiEA-8W2vz2bVZCzPv-S6CNDlbxNktEkOtTAg0XXiZ0ghLk"
    )*/
  ).map(setToken)
}
