package pdi.jwt

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.InvalidKeySpecException
import java.security.{KeyPair, KeyPairGenerator}
import java.util.Base64

class JwtUtilsKeysSpec extends munit.FunSuite with Fixture {
  private val keyPair: KeyPair = {
    val generator = KeyPairGenerator.getInstance(JwtUtils.RSA)
    generator.initialize(2048)
    generator.generateKeyPair()
  }

  private def pem(label: String, der: Array[Byte], eol: String): String = {
    val body = Base64.getMimeEncoder(64, eol.getBytes("UTF-8")).encodeToString(der)
    s"-----BEGIN $label-----$eol$body$eol-----END $label-----$eol"
  }

  test("parsePrivateKey should parse a PKCS#8 RSA private key PEM") {
    val parsed = JwtUtils.parsePrivateKey(
      pem("PRIVATE KEY", keyPair.getPrivate.getEncoded, "\n"),
      JwtUtils.RSA
    )
    assertEquals(parsed.getEncoded.toSeq, keyPair.getPrivate.getEncoded.toSeq)
    assertEquals(
      parsed.asInstanceOf[RSAPrivateKey].getModulus,
      keyPair.getPublic.asInstanceOf[RSAPublicKey].getModulus
    )
  }

  test("parsePublicKey should parse an X.509 RSA public key PEM") {
    val parsed =
      JwtUtils.parsePublicKey(pem("PUBLIC KEY", keyPair.getPublic.getEncoded, "\n"), JwtUtils.RSA)
    assertEquals(parsed, keyPair.getPublic)
  }

  test("parsePublicKey should parse the fixture RSA public key PEM") {
    val parsed = JwtUtils.parsePublicKey(publicKeyRSA, JwtUtils.RSA).asInstanceOf[RSAPublicKey]
    assertEquals(parsed.getPublicExponent, java.math.BigInteger.valueOf(65537))
    assertEquals(parsed.getModulus.bitLength, 2048)
  }

  test("parsePrivateKey and parsePublicKey should accept CRLF line endings") {
    val privatePem = pem("PRIVATE KEY", keyPair.getPrivate.getEncoded, "\r\n")
    val publicPem = pem("PUBLIC KEY", keyPair.getPublic.getEncoded, "\r\n")
    assert(privatePem.contains("\r\n"))
    assertEquals(JwtUtils.parsePrivateKey(privatePem, JwtUtils.RSA), keyPair.getPrivate)
    assertEquals(JwtUtils.parsePublicKey(publicPem, JwtUtils.RSA), keyPair.getPublic)
  }

  test("parsePrivateKey and parsePublicKey should accept bare base64") {
    val publicBase64 = Base64.getEncoder.encodeToString(keyPair.getPublic.getEncoded)
    assertEquals(JwtUtils.parsePublicKey(publicBase64, JwtUtils.RSA), keyPair.getPublic)
  }

  test("parsePrivateKey and parsePublicKey should throw on base64 that is not a key") {
    val notAKey = pem("PUBLIC KEY", "this is not a key".getBytes("UTF-8"), "\n")
    val publicFailure =
      intercept[InvalidKeySpecException](JwtUtils.parsePublicKey(notAKey, JwtUtils.RSA))
    val privateFailure =
      intercept[InvalidKeySpecException](JwtUtils.parsePrivateKey(notAKey, JwtUtils.RSA))
    assertNotEquals(publicFailure, privateFailure)
  }

  test("parsePrivateKey and parsePublicKey should throw on input that is not base64") {
    val publicFailure =
      intercept[IllegalArgumentException](
        JwtUtils.parsePublicKey("*** not base64 ***", JwtUtils.RSA)
      )
    val privateFailure =
      intercept[IllegalArgumentException](
        JwtUtils.parsePrivateKey("*** not base64 ***", JwtUtils.RSA)
      )
    assertNotEquals(publicFailure, privateFailure)
  }

  test("rsaPublicKey should rebuild a key from its modulus and exponent") {
    val original = keyPair.getPublic.asInstanceOf[RSAPublicKey]
    val rebuilt = JwtUtils.rsaPublicKey(original.getModulus, original.getPublicExponent)
    assertEquals(rebuilt, original)
    assertEquals(rebuilt.getEncoded.toSeq, original.getEncoded.toSeq)
  }
}
