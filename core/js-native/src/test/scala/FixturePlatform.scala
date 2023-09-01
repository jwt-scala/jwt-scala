package pdi.jwt

trait FixturePlatform extends ClockFixture {

  val Ed25519 = "Ed25519"

  val secretKey =
    "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
  val secretKeyBytes = JwtUtils.bytify(secretKey)

  val claim = s"""{"iss":"joe","exp":$expiration,"http://example.com/is_root":true}"""
  val claimClass = JwtClaim(
    """{"http://example.com/is_root":true}""",
    issuer = Option("joe"),
    expiration = Option(expiration)
  )
  val claim64 =
    "eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"

  val headerEmpty = """{"alg":"none"}"""
  val headerClassEmpty = JwtHeader()
  val header64Empty = "eyJhbGciOiJub25lIn0"

  val tokenEmpty = header64Empty + "." + claim64 + "."

  val headerWithSpaces = """{"alg"  :   "none"}"""
  val claimWithSpaces = """{"nbf"  :0  , "foo"  : "bar"  , "exp":    32086368000}"""
  val tokenWithSpaces =
    JwtBase64.encodeString(headerWithSpaces) + "." + JwtBase64.encodeString(claimWithSpaces) + "."

  val publicKeyRSA = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvzoCEC2rpSpJQaWZbUml
sDNwp83Jr4fi6KmBWIwnj1MZ6CUQ7rBasuLI8AcfX5/10scSfQNCsTLV2tMKQaHu
vyrVfwY0dINk+nkqB74QcT2oCCH9XduJjDuwWA4xLqAKuF96FsIes52opEM50W7/
W7DZCKXkC8fFPFj6QF5ZzApDw2Qsu3yMRmr7/W9uWeaTwfPx24YdY7Ah+fdLy3KN
40vXv9c4xiSafVvnx9BwYL7H1Q8NiK9LGEN6+JSWfgckQCs6UUBOXSZdreNN9zbQ
Cwyzee7bOJqXUDAuLcFARzPw1EsZAyjVtGCKIQ0/btqK+jFunT2NBC8RItanDZpp
tQIDAQAB
-----END PUBLIC KEY-----"""

  val privateKeyRSA = """-----BEGIN RSA PRIVATE KEY-----
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
-----END RSA PRIVATE KEY-----"""

  val S = BigInt(
    "1ed498eedf499e5dd12b1ab94ee03d1a722eaca3ed890630c8b25f1015dd4ec5630a02ddb603f3248a3b87c88637e147ecc7a6e2a1c2f9ff1103be74e5d42def37d",
    16
  )
  val X = BigInt(
    "16528ac15dc4c8e0559fad628ac3ffbf5c7cfefe12d50a97c7d088cc10b408d4ab03ac0d543bde862699a74925c1f2fe7c247c00fddc1442099dfa0671fc032e10a",
    16
  )
  val Y = BigInt(
    "b7f22b3c1322beef766cadd1a5f0363840195b7be10d9a518802d8d528e03bc164c9588c5e63f1473d05195510676008b6808508539367d2893e1aa4b7cb9f9dab",
    16
  )

  val privateKeyEd25519 = "MC4CAQAwBQYDK2VwBCIEIHf3EQMqRKbBYOEjmrRm6Zu5hIYombr3DoWaRjZqK7uv"
  val publicKeyEd25519 = "MCowBQYDK2VwAyEAMGx9f797iAEdcI/QULMQFxgnt3ANZAqlTHavvAf3nD4="
}
