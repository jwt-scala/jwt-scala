package pdi.scala.jwt

import org.scalatest._

import scala.util.{Success, Failure}

class JwtSpec extends UnitSpec {
  describe("Jwt") {
    val key = Option("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")

    val claim = """{"iss":"joe","exp":1300819380,"http://example.com/is_root":true}"""
    val claimClass = JwtClaim("""{"http://example.com/is_root":true}""", issuer = Option("joe"), expiration = Option(1300819380))
    val claim64 = "eyJpc3MiOiJqb2UiLCJleHAiOjEzMDA4MTkzODAsImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ=="

    val headerMD5 = """{"typ":"JWT","alg":"HmacMD5"}"""
    val headerMD5Class = JwtHeader("HmacMD5", "JWT")
    val headerMD564 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjTUQ1In0="
    val signatureMD5 = "eW2omY9kpadPg_M5ECg_SQ=="
    val tokenMD5 = Seq(headerMD564, claim64, signatureMD5).mkString(".")

    val headerMD5Alt = """{"typ":"JWT","alg":"HMD5"}"""
    val headerMD5AltClass = JwtHeader("HMD5", "JWT")
    val headerMD564Atl = "eyJ0eXAiOiJKV1QiLCJhbGciOiJITUQ1In0="
    val signatureMD5Alt = "DN76C8aGlSM-Rwy75HHnFA=="
    val tokenMD5Alt = Seq(headerMD564Atl, claim64, signatureMD5Alt).mkString(".")

    val headerSHA1 = """{"typ":"JWT","alg":"HmacSHA1"}"""
    val headerSHA1Class = JwtHeader("HmacSHA1", "JWT")
    val header64SHA1 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMSJ9"
    val signatureSHA1 = "FbaxYjZdyrsZcfzJNqgD0DrXP8g="
    val tokenSHA1 = Seq(header64SHA1, claim64, signatureSHA1).mkString(".")

    val headerSHA1Alt = """{"typ":"JWT","alg":"HS1"}"""
    val headerSHA1AltClass = JwtHeader("HS1", "JWT")
    val header64SHA1Alt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzEifQ=="
    val signatureSHA1Alt = "ehprddUi3mH2cXVN35pGNHkeSBk="
    val tokenSHA1Alt = Seq(header64SHA1Alt, claim64, signatureSHA1Alt).mkString(".")

    val headerSHA256 = """{"typ":"JWT","alg":"HmacSHA256"}"""
    val headerSHA256Class = JwtHeader("HmacSHA256", "JWT")
    val header64SHA256 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBMjU2In0="
    val signatureSHA256 = "8dFiIcid4qZ5zHjfRNpIbyrc5GWQbddz5UZVkqzvQKU="
    val tokenSHA256 = Seq(header64SHA256, claim64, signatureSHA256).mkString(".")

    val headerSHA256Alt = """{"typ":"JWT","alg":"HS256"}"""
    val headerSHA256AltClass = JwtHeader("HS256", "JWT")
    val header64SHA256Alt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
    val signatureSHA256Alt = "hZOTRuD0YLWUYyhhDp0KXfFCtgAxiTzfwyFXLuPldEk="
    val tokenSHA256Alt = Seq(header64SHA256Alt, claim64, signatureSHA256Alt).mkString(".")

    val headerSHA512 = """{"typ":"JWT","alg":"HmacSHA512"}"""
    val headerSHA512Class = JwtHeader("HmacSHA512", "JWT")
    val header64SHA512 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIbWFjU0hBNTEyIn0="
    val signatureSHA512 = "DRtzJb01Gk1tHPxgiK13gAnsMev4hF6Otr7oNI7oJp0YNrIkYdnz-ikf3OhwSNWdvpHslv0oH5jEsi8LwKkg7A=="
    val tokenSHA512 = Seq(header64SHA512, claim64, signatureSHA512).mkString(".")

    val headerSHA512Alt = """{"typ":"JWT","alg":"HS512"}"""
    val headerSHA512AltClass = JwtHeader("HS512", "JWT")
    val header64SHA512Alt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9"
    val signatureSHA512Alt = "3M6Q4hImCsJj-c7mnN6NFnmnoDSv5n1UF8WUNFQ_pvFaRYGNKiHDg_ZJhUYapTGzp6w-0fsXfp6WVHHGy--MJA=="
    val tokenSHA512Alt = Seq(header64SHA512Alt, claim64, signatureSHA512Alt).mkString(".")

    it("should encode") {
      assertResult(tokenMD5, "MD5") { Jwt.encode(headerMD5, claim, key.get, "HmacMD5") }
      assertResult(tokenMD5Alt, "MD5 Alt") { Jwt.encode(headerMD5Alt, claim, key.get, "HMD5") }
      assertResult(tokenSHA1, "SHA1") { Jwt.encode(headerSHA1, claim, key.get, "HmacSHA1") }
      assertResult(tokenSHA1Alt, "SHA1 Alt") { Jwt.encode(headerSHA1Alt, claim, key.get, "HS1") }
      assertResult(tokenSHA256, "SHA256") { Jwt.encode(headerSHA256, claim, key.get, "HmacSHA256") }
      assertResult(tokenSHA256Alt, "SHA256 Alt") { Jwt.encode(headerSHA256Alt, claim, key.get, "HS256") }
      assertResult(tokenSHA512, "SHA512") { Jwt.encode(headerSHA512, claim, key.get, "HmacSHA512") }
      assertResult(tokenSHA512Alt, "SHA512 Alt") { Jwt.encode(headerSHA512Alt, claim, key.get, "HS512") }
    }

    it("should encode case class") {
      assertResult(tokenMD5, "MD5") { Jwt.encode(headerMD5Class, claimClass, key.get) }
      assertResult(tokenMD5Alt, "MD5 Alt") { Jwt.encode(headerMD5AltClass, claimClass, key.get) }
      assertResult(tokenSHA1, "SHA1") { Jwt.encode(headerSHA1Class, claimClass, key.get) }
      assertResult(tokenSHA1Alt, "SHA1 Alt") { Jwt.encode(headerSHA1AltClass, claimClass, key.get) }
      assertResult(tokenSHA256, "SHA256") { Jwt.encode(headerSHA256Class, claimClass, key.get) }
      assertResult(tokenSHA256Alt, "SHA256 Alt") { Jwt.encode(headerSHA256AltClass, claimClass, key.get) }
      assertResult(tokenSHA512, "SHA512") { Jwt.encode(headerSHA512Class, claimClass, key.get) }
      assertResult(tokenSHA512Alt, "SHA512 Alt") { Jwt.encode(headerSHA512AltClass, claimClass, key.get) }
    }

    it("should decodeRawAll") {
      assertResult(Success((headerMD5, claim, Some(signatureMD5))), "MD5") { Jwt.decodeRawAll(tokenMD5) }
      assertResult(Success((headerMD5Alt, claim, Some(signatureMD5Alt))), "MD5Alt") {
        Jwt.decodeRawAll(tokenMD5Alt)
      }
      assertResult(Success((headerSHA1, claim, Some(signatureSHA1))), "SHA1") { Jwt.decodeRawAll(tokenSHA1) }
      assertResult(Success((headerSHA1Alt, claim, Some(signatureSHA1Alt))), "SHA1Alt") {
        Jwt.decodeRawAll(tokenSHA1Alt)
      }
      assertResult(Success((headerSHA256, claim, Some(signatureSHA256))), "SHA256") { Jwt.decodeRawAll(tokenSHA256) }
      assertResult(Success((headerSHA256Alt, claim, Some(signatureSHA256Alt))), "SHA256Alt") {
        Jwt.decodeRawAll(tokenSHA256Alt)
      }
      assertResult(Success((headerSHA512, claim, Some(signatureSHA512))), "SHA512") { Jwt.decodeRawAll(tokenSHA512) }
      assertResult(Success((headerSHA512Alt, claim, Some(signatureSHA512Alt))), "SHA512Alt") {
        Jwt.decodeRawAll(tokenSHA512Alt)
      }
    }

    it("should decodeRaw") {
      assertResult(Success((claim)), "MD5") { Jwt.decodeRaw(tokenMD5) }
      assertResult(Success((claim)), "MD5Alt") { Jwt.decodeRaw(tokenMD5Alt) }
      assertResult(Success((claim)), "SHA1") { Jwt.decodeRaw(tokenSHA1) }
      assertResult(Success((claim)), "SHA1Alt") { Jwt.decodeRaw(tokenSHA1Alt) }
      assertResult(Success((claim)), "SHA256") { Jwt.decodeRaw(tokenSHA256) }
      assertResult(Success((claim)), "SHA256Alt") { Jwt.decodeRaw(tokenSHA256Alt) }
      assertResult(Success((claim)), "SHA512") { Jwt.decodeRaw(tokenSHA512) }
      assertResult(Success((claim)), "SHA512Alt") { Jwt.decodeRaw(tokenSHA512Alt) }
    }

    it("should decodeAll") {
      assertResult(Success((headerMD5, claim, Some(signatureMD5))), "MD5") { Jwt.decodeAll(tokenMD5, key) }
      assertResult(Success((headerMD5Alt, claim, Some(signatureMD5Alt))), "MD5Alt") {
        Jwt.decodeAll(tokenMD5Alt, key)
      }
      assertResult(Success((headerSHA1, claim, Some(signatureSHA1))), "SHA1") { Jwt.decodeAll(tokenSHA1, key) }
      assertResult(Success((headerSHA1Alt, claim, Some(signatureSHA1Alt))), "SHA1Alt") {
        Jwt.decodeAll(tokenSHA1Alt, key)
      }
      assertResult(Success((headerSHA256, claim, Some(signatureSHA256))), "SHA256") { Jwt.decodeAll(tokenSHA256, key) }
      assertResult(Success((headerSHA256Alt, claim, Some(signatureSHA256Alt))), "SHA256Alt") {
        Jwt.decodeAll(tokenSHA256Alt, key)
      }
      assertResult(Success((headerSHA512, claim, Some(signatureSHA512))), "SHA512") { Jwt.decodeAll(tokenSHA512, key) }
      assertResult(Success((headerSHA512Alt, claim, Some(signatureSHA512Alt))), "SHA512Alt") {
        Jwt.decodeAll(tokenSHA512Alt, key)
      }
    }

    it("should decode") {
      assertResult(Success(claim), "MD5") { Jwt.decode(tokenMD5, key) }
      assertResult(Success(claim), "MD5Alt") { Jwt.decode(tokenMD5Alt, key) }
      assertResult(Success(claim), "SHA1") { Jwt.decode(tokenSHA1, key) }
      assertResult(Success(claim), "SHA1Alt") { Jwt.decode(tokenSHA1Alt, key) }
      assertResult(Success(claim), "SHA256") { Jwt.decode(tokenSHA256, key) }
      assertResult(Success(claim), "SHA256Alt") { Jwt.decode(tokenSHA256Alt, key) }
      assertResult(Success(claim), "SHA512") { Jwt.decode(tokenSHA512, key) }
      assertResult(Success(claim), "SHA512Alt") { Jwt.decode(tokenSHA512Alt, key) }
    }

    it("should validate") {

    }
  }
}
