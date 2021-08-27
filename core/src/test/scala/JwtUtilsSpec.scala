package pdi.jwt

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}

import org.scalacheck.Gen
import org.scalacheck.Prop._
import pdi.jwt.exceptions.JwtSignatureFormatException

case class TestObject(value: String) {
  override def toString(): String = this.value
}

class JwtUtilsSpec extends munit.ScalaCheckSuite with Fixture {
  val ENCODING = JwtUtils.ENCODING

  test("hashToJson should transform a seq of tuples to a valid JSON") {
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
      assertEquals(value._1, JwtUtils.hashToJson(value._2), "at index " + index)
    }
  }

  test("mergeJson should correctly merge 2 JSONs") {
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
      assertEquals(value._1, JwtUtils.mergeJson(value._2, value._3: _*), "at index " + index)
    }
  }

  test("Claim.toJson should correctly encode a Claim to JSON") {
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

    assertEquals(jsonClaim, claim.toJson)
  }

  test("transcodeSignatureToDER should throw JwtValidationException if signature is too long") {
    val signature = JwtUtils.bytify(
      "AU6-jw28DX1QMY0Ar8CTcnIAc0WKGe3nNVHkE7ayHSxvOLxE5YQSiZtbPn3y-vDHoQCOMId4rPdIJhD_NOUqnH_rAKA5w9ZlhtW0GwgpvOg1_5oLWnWXQvPjJjC5YsLqEssoMITtOmfkBsQMgLAF_LElaaCWhkJkOCtcZmroUW_b5CXB"
    )
    interceptMessage[JwtSignatureFormatException]("Invalid ECDSA signature format") {
      JwtUtils.transcodeSignatureToDER(signature ++ signature)
    }
  }

  test("transcodeSignatureToDER should transocde empty signature") {
    val signature: Array[Byte] = Array[Byte](0)
    JwtUtils.transcodeSignatureToDER(signature)
  }

  test("transcodeSignatureToConcat should throw JwtValidationException if length incorrect") {
    val signature = JwtUtils.bytify(
      "MIEAAGg3OVb/ZeX12cYrhK3c07TsMKo7Kc6SiqW++4CAZWCX72DkZPGTdCv2duqlupsnZL53hiG3rfdOLj8drndCU+KHGrn5EotCATdMSLCXJSMMJoHMM/ZPG+QOHHPlOWnAvpC1v4lJb32WxMFNz1VAIWrl9Aa6RPG1GcjCTScKjvEE"
    )
    interceptMessage[JwtSignatureFormatException]("Invalid ECDSA signature format") {
      JwtUtils.transcodeSignatureToConcat(signature, 132)
    }
  }

  test(
    "transcodeSignatureToConcat should throw JwtValidationException if signature is incorrect "
  ) {
    val signature = JwtUtils.bytify(
      "MIGBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    )
    interceptMessage[JwtSignatureFormatException]("Invalid ECDSA signature format") {
      JwtUtils.transcodeSignatureToConcat(signature, 132)
    }
  }

  test(
    "transcodeSignatureToConcat should throw JwtValidationException if signature is incorrect 2"
  ) {
    val signature = JwtUtils.bytify(
      "MIGBAD4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    )
    interceptMessage[JwtSignatureFormatException]("Invalid ECDSA signature format") {
      JwtUtils.transcodeSignatureToConcat(signature, 132)
    }
  }

  test("transcodeSignatureToConcat and transcodeSignatureToDER should be symmetric") {
    val signature = JwtUtils.bytify(
      "AbxLPbA3dm9V0jt6c_ahf8PYioFvnryTe3odgolhcgwBUl4ifpwUBJ--GgiXC8vms45c8vI40ZSdkm5NoNn1wTHOAfkepNy-RRKHmBzAoWrWmBIb76yPa0lsjdAPEAXcbGfaQV8pKq7W10dpB2B-KeJxVonMuCLJHPuqsUl9S7CfASu2"
    )
    val dER: Array[Byte] = JwtUtils.transcodeSignatureToDER(signature)
    val result = JwtUtils.transcodeSignatureToConcat(
      dER,
      JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512)
    )
    assertArrayEquals(signature, result)
  }

  test(
    "transcodeSignatureToConcat and transcodeSignatureToDER should be symmetric for generated tokens"
  ) {
    val ecGenSpec = new ECGenParameterSpec(ecCurveName)
    val generatorEC = KeyPairGenerator.getInstance(JwtUtils.ECDSA)
    generatorEC.initialize(ecGenSpec, new SecureRandom())
    val randomECKey = generatorEC.generateKeyPair()
    val header = """{"typ":"JWT","alg":"ES512"}"""
    val claim = """{"test":"t"}"""

    val signature = Jwt(validTimeClock)
      .encode(header, claim, randomECKey.getPrivate, JwtAlgorithm.ES512)
      .split("\\.")(2)
    assertEquals(
      signature,
      JwtUtils.stringify(
        JwtUtils.transcodeSignatureToConcat(
          JwtUtils.transcodeSignatureToDER(JwtUtils.bytify(signature)),
          JwtUtils.getSignatureByteArrayLength(JwtAlgorithm.ES512)
        )
      )
    )
  }

  test("splitString should do nothing") {
    forAll(Gen.asciiStr.suchThat(s => s.nonEmpty && !s.contains('a'))) { (value: String) =>
      assertArrayEquals(
        JwtUtils.splitString(value, 'a'),
        Array(value)
      )
    }
  }

  test("splitString should split once") {
    assertArrayEquals(JwtUtils.splitString("qwertyAzxcvb", 'A'), Array("qwerty", "zxcvb"))
  }

  test("splitString should split a token") {
    assertArrayEquals(
      JwtUtils.splitString("header.claim.signature", '.'),
      Array("header", "claim", "signature")
    )
  }

  test("splitString should split a token without signature") {
    assertArrayEquals(JwtUtils.splitString("header.claim", '.'), Array("header", "claim"))
  }

  test("splitString should split a token with an empty signature") {
    assertArrayEquals(JwtUtils.splitString("header.claim.", '.'), Array("header", "claim"))
  }

  test("splitString should split a token with an empty header") {
    assertArrayEquals(JwtUtils.splitString(".claim.", '.'), Array("", "claim"))
  }

  test("splitString should be the same as normal split") {
    var token = "header.claim.signature"
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = "header.claim."
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = "header.claim"
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = ".claim.signature"
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = ".claim."
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = "1"
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
    token = "a.b.c.d"
    assertArrayEquals(token.split("\\."), JwtUtils.splitString(token, '.'))
  }

  private def assertArrayEquals[A](arr1: Array[A], arr2: Array[A]): Unit = {
    assertEquals(arr1.toSeq, arr2.toSeq)
  }
}
