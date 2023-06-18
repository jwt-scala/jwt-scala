package pdi.jwt

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}

trait JwtUtilsPlatformSpec { self: munit.ScalaCheckSuite with Fixture =>

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
