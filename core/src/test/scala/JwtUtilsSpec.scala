package pdi.jwt

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}

import pdi.jwt.exceptions.JwtSignatureFormatException
import org.scalatest.enablers.Messaging.messagingNatureOfAnyRefWithGetMessageMethod

case class TestObject(value: String) {
  override def toString(): String = this.value
}

class JwtUtilsSpec extends UnitSpec with ClockFixture {
  val ENCODING = JwtUtils.ENCODING

  describe("JwtUtils") {
    describe("hashToJson") {
      it("should transform a seq of tuples to a valid JSON") {
        val values: Seq[(String, Seq[(String, Any)])] = Seq(
          """{"a":"b","c":1,"d":true,"e":2,"f":3.4,"g":5.6}""" -> Seq(
            "a" -> "b",
            "c" -> 1,
            "d" -> true,
            "e" -> 2L,
            "f" -> 3.4f,
            "g" -> 5.6
          ),
          "{}" -> Seq(),
          """{"a\"b":"a\"b","c\"d":"c\"d","e\"f":["e\"f","e\"f"]}""" -> Seq(
            """a"b""" -> """a"b""",
            """c"d""" -> TestObject("""c"d"""),
            """e"f""" -> Seq("""e"f""", TestObject("""e"f"""))
          )
        )

        values.zipWithIndex.foreach { case (value, index) =>
          assertResult(value._1, "at index " + index) { JwtUtils.hashToJson(value._2) }
        }
      }
    }

    describe("mergeJson") {
      it("should correctly merge 2 JSONs") {
        val values: Seq[(String, String, Seq[String])] = Seq(
          ("{}", "{}", Seq("{}")),
          ("""{"a":1}""", """{"a":1}""", Seq("")),
          ("""{"a":1}""", """{"a":1}""", Seq("{}")),
          ("""{"a":1}""", """{}""", Seq("""{"a":1}""")),
          ("""{"a":1}""", "", Seq("""{"a":1}""")),
          ("""{"a":1,"b":2}""", """{"a":1}""", Seq("""{"b":2}""")),
          ("""{"a":1,"b":2,"c":"d"}""", """{"a":1}""", Seq("""{"b":2}""", """{"c":"d"}"""))
        )

        values.zipWithIndex.foreach { case (value, index) =>
          assertResult(value._1, "at index " + index) { JwtUtils.mergeJson(value._2, value._3: _*) }
        }
      }
    }

    describe("Claim.toJson") {
      it("should correctly encode a Claim to JSON") {
        val claim = JwtClaim(
          issuer = Some(""),
          audience = Some(Set("")),
          subject = Some("da1b3852-6827-11e9-a923-1681be663d3e"),
          expiration = Some(1597914901),
          issuedAt = Some(1566378901),
          content = "{\"a\":\"da1b3852-6827-11e9-a923-1681be663d3e\",\"b\":123.34}"
        )

        val jsonClaim =
          """{"iss":"","sub":"da1b3852-6827-11e9-a923-1681be663d3e","aud":"","exp":1597914901,"iat":1566378901,"a":"da1b3852-6827-11e9-a923-1681be663d3e","b":123.34}"""

        assertResult(jsonClaim) { claim.toJson }
      }
    }

    describe("transcodeSignatureToDER") {
      it("should throw JwtValidationException if signature is too long") {
        val signature = JwtUtils.bytify(
          "AU6-jw28DX1QMY0Ar8CTcnIAc0WKGe3nNVHkE7ayHSxvOLxE5YQSiZtbPn3y-vDHoQCOMId4rPdIJhD_NOUqnH_rAKA5w9ZlhtW0GwgpvOg1_5oLWnWXQvPjJjC5YsLqEssoMITtOmfkBsQMgLAF_LElaaCWhkJkOCtcZmroUW_b5CXB"
        )
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToDER(signature ++ signature)
        } should have message "Invalid ECDSA signature format"
      }

      it("should transocde empty signature") {
        val signature: Array[Byte] = Array[Byte](0)
        JwtUtils.transcodeSignatureToDER(signature)
      }
    }

    describe("transcodeSignatureToConcat") {
      it("should throw JwtValidationException if length incorrect") {
        val signature = JwtUtils.bytify(
          "MIEAAGg3OVb/ZeX12cYrhK3c07TsMKo7Kc6SiqW++4CAZWCX72DkZPGTdCv2duqlupsnZL53hiG3rfdOLj8drndCU+KHGrn5EotCATdMSLCXJSMMJoHMM/ZPG+QOHHPlOWnAvpC1v4lJb32WxMFNz1VAIWrl9Aa6RPG1GcjCTScKjvEE"
        )
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }

      it("should throw JwtValidationException if signature is incorrect ") {
        val signature = JwtUtils.bytify(
          "MIGBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }

      it("should throw JwtValidationException if signature is incorrect 2") {
        val signature = JwtUtils.bytify(
          "MIGBAD4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }
    }

    describe("transcodeSignatureToConcat and transcodeSignatureToDER") {
      it("should be symmetric") {
        val signature = JwtUtils.bytify(
          "AbxLPbA3dm9V0jt6c_ahf8PYioFvnryTe3odgolhcgwBUl4ifpwUBJ--GgiXC8vms45c8vI40ZSdkm5NoNn1wTHOAfkepNy-RRKHmBzAoWrWmBIb76yPa0lsjdAPEAXcbGfaQV8pKq7W10dpB2B-KeJxVonMuCLJHPuqsUl9S7CfASu2"
        )
        val dER: Array[Byte] = JwtUtils.transcodeSignatureToDER(signature)
        val result = JwtUtils.transcodeSignatureToConcat(
          dER,
          JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512)
        )
        assertResult(signature) {
          result
        }
      }

      it("should be symmetric for generated tokens") {
        val ecGenSpec = new ECGenParameterSpec(ecCurveName)
        val generatorEC = KeyPairGenerator.getInstance(JwtUtils.ECDSA)
        generatorEC.initialize(ecGenSpec, new SecureRandom())
        val randomECKey = generatorEC.generateKeyPair()
        val header = """{"typ":"JWT","alg":"ES512"}"""
        val claim = """{"test":"t"}"""

        val signature = Jwt(validTimeClock)
          .encode(header, claim, randomECKey.getPrivate, JwtAlgorithm.ES512)
          .split("\\.")(2)
        assertResult(signature) {
          JwtUtils.stringify(
            JwtUtils.transcodeSignatureToConcat(
              JwtUtils.transcodeSignatureToDER(JwtUtils.bytify(signature)),
              JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512)
            )
          )
        }
      }
    }

    describe("splitString") {
      it("should do nothing") {
        assertResult(Array("qwerty")) { JwtUtils.splitString("qwerty", 'a') }
      }

      it("should split once") {
        assertResult(Array("qwerty", "zxcvb")) { JwtUtils.splitString("qwertyAzxcvb", 'A') }
      }

      it("should split a token") {
        assertResult(Array("header", "claim", "signature")) {
          JwtUtils.splitString("header.claim.signature", '.')
        }
      }

      it("should split a token without signature") {
        assertResult(Array("header", "claim")) { JwtUtils.splitString("header.claim", '.') }
      }

      it("should split a token with an empty signature") {
        assertResult(Array("header", "claim")) { JwtUtils.splitString("header.claim.", '.') }
      }

      it("should split a token with an empty header") {
        assertResult(Array("", "claim")) { JwtUtils.splitString(".claim.", '.') }
      }

      it("should be the same as normal split") {
        var token = "header.claim.signature"
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = "header.claim."
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = "header.claim"
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = ".claim.signature"
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = ".claim."
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = "1"
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
        token = "a.b.c.d"
        assertResult(token.split("\\.")) { JwtUtils.splitString(token, '.') }
      }
    }
  }
}
