package pdi.jwt

import java.math.BigInteger
import java.security.interfaces.{ECPublicKey, RSAPublicKey}
import java.security.spec.{ECGenParameterSpec, ECPoint, ECPublicKeySpec}
import java.security.{KeyFactory, KeyPair, KeyPairGenerator, Security}

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

  def ecPublic(curve: EcCurve, x: BigInteger, y: BigInteger): ECPublicKey =
    KeyFactory
      .getInstance("EC")
      .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curve.parameterSpec))
      .asInstanceOf[ECPublicKey]

  // An EC P-384 public key whose affine X coordinate has a leading zero byte (< 48 bytes)
  val ShortP384Key: ECPublicKey = ecPublic(
    EcCurve.P384,
    new BigInteger(
      "3da824a12d6c76fb729abe3dac8098cda750af8912d5af885fdc118067f75e72baa47ab0ddf7a33d63c42900690cdb",
      16
    ),
    new BigInteger(
      "717b88dcaad09ebe3ba96abbfa4e1f8ec6d6ea70f6699aa7bc0d59089f7d6b4bf34380e9cb3cb1e02a6115d1cf68f164",
      16
    )
  )

  // An EC P-521 public key whose affine X and Y coordinates both have a leading zero byte (< 66 bytes)
  val ShortP521Key: ECPublicKey = ecPublic(
    EcCurve.P521,
    new BigInteger(
      "ac78d8748cf3bec95d75b35cf0f4369f736a32a9c0155a8c4690e1eff093ab46fe59c522cc684131e18ae786d508c4679b949defaff1eed6faea2b3918f24576da",
      16
    ),
    new BigInteger(
      "b4baaba0feb4d4b47e42ff09ec9d2d48dba9f235f663c003137accb10fa5d0899e732e4914e5b9c31123b4a255fe4d525f536359135088ff18a6f67469fd5f0e16",
      16
    )
  )
}
