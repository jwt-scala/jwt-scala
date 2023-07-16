package pdi.jwt

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}

trait JwtUtilsPlatformSpec { self: munit.ScalaCheckSuite with Fixture =>

  test("hashToJson should transform a seq of tuples to a valid JSON") {
    val values: Seq[(String, Seq[(String, Any)])] = Seq(
      """{"a":"b","c":1,"d":true,"e":2,"f":3.4,"g":5.6}""" -> Seq(
        "a" -> "b",
        "c" -> 1,
        "d" -> true,
        "e" -> 2L,
        "f" -> 3.4f, // JVM only (JS does not handle floats correctly)
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
}
