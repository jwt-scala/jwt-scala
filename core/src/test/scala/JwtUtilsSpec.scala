package pdi.jwt

import java.security.{KeyPairGenerator, SecureRandom}
import java.security.spec.ECGenParameterSpec

import pdi.jwt.exceptions.JwtSignatureFormatException

case class TestObject(value: String) {
  override def toString(): String = this.value
}

class JwtUtilsSpec extends UnitSpec {
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

        values.zipWithIndex.foreach {
          case (value, index) => assertResult(value._1, "at index "+index) { JwtUtils.hashToJson(value._2) }
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

        values.zipWithIndex.foreach {
          case (value, index) => assertResult(value._1, "at index "+index) { JwtUtils.mergeJson(value._2, value._3: _*) }
        }
      }
    }

    val signKey = Option("secret")
    val signMessage = """{"alg": "algo"}.{"user": 1, "admin": true, "value": "foo"}"""

    // Seq[(result, algo)]
    val signValues: Seq[(String, String)] = Seq(
      ("媉㶩஥ᐎ䗼ⲑΠ", "HmacMD5"),
      ("媉㶩஥ᐎ䗼ⲑΠ", "HMD5"),
      ("ﹰﱉ녙죀빊署▢륧婍", "HmacSHA1")
    )

    /*describe("sign byte array") {
      it("should correctly handle string") {
        signValues.foreach {
          value => assertResult(value._1.getBytes(ENCODING)) { JwtUtils.sign(signMessage.getBytes(ENCODING), signKey, Option(value._2)) }
        }
      }
    }

    describe("sign string") {
      it("should correctly handle string") {
        signValues.foreach {
          value => assertResult(value._1.getBytes(ENCODING)) { JwtUtils.sign(signMessage, signKey, Option(value._2)) }
        }
      }
    }*/

    describe("transcodeSignatureToDER") {
      it("should throw JwtValidationException if signature is too long") {
        val signature = JwtUtils.bytify("AU6-jw28DX1QMY0Ar8CTcnIAc0WKGe3nNVHkE7ayHSxvOLxE5YQSiZtbPn3y-vDHoQCOMId4rPdIJhD_NOUqnH_rAKA5w9ZlhtW0GwgpvOg1_5oLWnWXQvPjJjC5YsLqEssoMITtOmfkBsQMgLAF_LElaaCWhkJkOCtcZmroUW_b5CXB")
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
        val signature = JwtUtils.bytify("MIEAAGg3OVb/ZeX12cYrhK3c07TsMKo7Kc6SiqW++4CAZWCX72DkZPGTdCv2duqlupsnZL53hiG3rfdOLj8drndCU+KHGrn5EotCATdMSLCXJSMMJoHMM/ZPG+QOHHPlOWnAvpC1v4lJb32WxMFNz1VAIWrl9Aa6RPG1GcjCTScKjvEE")
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }

      it("should throw JwtValidationException if signature is incorrect ") {
        val signature = JwtUtils.bytify("MIGBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }

      it("should throw JwtValidationException if signature is incorrect 2") {
        val signature = JwtUtils.bytify("MIGBAD4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        the[JwtSignatureFormatException] thrownBy {
          JwtUtils.transcodeSignatureToConcat(signature, 132)
        } should have message "Invalid ECDSA signature format"
      }
    }

    describe("transcodeSignatureToConcat and transcodeSignatureToDER") {
      it("should be symmetric") {
        val signature = JwtUtils.bytify("AbxLPbA3dm9V0jt6c_ahf8PYioFvnryTe3odgolhcgwBUl4ifpwUBJ--GgiXC8vms45c8vI40ZSdkm5NoNn1wTHOAfkepNy-RRKHmBzAoWrWmBIb76yPa0lsjdAPEAXcbGfaQV8pKq7W10dpB2B-KeJxVonMuCLJHPuqsUl9S7CfASu2")
        val dER: Array[Byte] = JwtUtils.transcodeSignatureToDER(signature)
        val result = JwtUtils.transcodeSignatureToConcat(dER, JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512))
        assertResult(signature) {
          result
        }
      }

      it("should be symmetric for generated tokens") {
        val ecGenSpec = new ECGenParameterSpec("P-521")
        val generatorEC = KeyPairGenerator.getInstance(JwtUtils.ECDSA, JwtUtils.PROVIDER)
        generatorEC.initialize(ecGenSpec, new SecureRandom())
        val randomECKey = generatorEC.generateKeyPair()
        val header = """{"typ":"JWT","alg":"ES512"}"""
        val claim = """{"test":"t"}"""

        val signature = Jwt.encode(header, claim, randomECKey.getPrivate, JwtAlgorithm.ES512).split("\\.")(2)
        assertResult(signature) {
          JwtUtils.stringify(JwtUtils.transcodeSignatureToConcat(JwtUtils.transcodeSignatureToDER(JwtUtils.bytify(signature)),
            JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512)))
        }
      }
    }


  }
}
