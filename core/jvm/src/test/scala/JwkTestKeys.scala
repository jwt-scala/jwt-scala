package pdi.jwt

import java.math.BigInteger
import java.security.interfaces.{ECPublicKey, RSAPublicKey}
import java.security.spec.ECGenParameterSpec
import java.security.{KeyPair, KeyPairGenerator, Security}

import org.bouncycastle.jce.provider.BouncyCastleProvider

object JwkTestKeys {

  // Ed25519 and Ed448 are only in the JDK since 15, Bouncycastle provides them before.
  if (Security.getProvider("BC") == null) {
    Security.addProvider(new BouncyCastleProvider())
    ()
  }

  val Rfc7638Modulus =
    "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPeb" +
      "WKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgn" +
      "Yb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44" +
      "-csFCur-kEgU8awapJzKnqDKgw"
  val Rfc7638Thumbprint = "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs"

  val Rfc8037Ed25519X = "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
  val Rfc8037Ed25519Thumbprint = "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k"

  def rsa(bits: Int = 2048): KeyPair = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(bits)
    generator.generateKeyPair()
  }

  def ec(curve: EcCurve): KeyPair = {
    val generator = KeyPairGenerator.getInstance("EC")
    generator.initialize(new ECGenParameterSpec(curve.jdkName))
    generator.generateKeyPair()
  }

  def okp(curve: OkpCurve): KeyPair = KeyPairGenerator.getInstance(curve.name).generateKeyPair()

  def unsignedBytes(value: BigInteger): Array[Byte] = {
    val bytes = value.toByteArray
    if (bytes.length > 1 && bytes(0) == 0) bytes.drop(1) else bytes
  }

  def rsaPublic(pair: KeyPair): RSAPublicKey = pair.getPublic.asInstanceOf[RSAPublicKey]

  def ecPublic(pair: KeyPair): ECPublicKey = pair.getPublic.asInstanceOf[ECPublicKey]
}
