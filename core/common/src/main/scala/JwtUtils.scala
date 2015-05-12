package pdi.jwt

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.{Security, Signature, KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import JwtAlgorithm._

object JwtUtils {
  val ENCODING = "UTF-8"
  val PROVIDER = "BC"
  val RSA = "RSA"
  val ECDSA = "ECDSA"

  if (Security.getProvider(PROVIDER) == null) {
    Security.addProvider(new BouncyCastleProvider())
  }

  def stringify(arr: Array[Byte]): String = new String(arr, ENCODING)
  def bytify(str: String): Array[Byte] = str.getBytes(ENCODING)

  def seqToJson(hash: Seq[(String, Any)]): String = hash.map {
    case (key, value: String) => "\"" + key + "\":\"" + value + "\""
    case (key, value: Boolean) => "\"" + key + "\":" + (if (value) { "true" } else { "false" })
    case (key, value: Double) => "\"" + key + "\":" + value.toString
    case (key, value: Short) => "\"" + key + "\":" + value.toString
    case (key, value: Float) => "\"" + key + "\":" + value.toString
    case (key, value: Long) => "\"" + key + "\":" + value.toString
    case (key, value: Int) => "\"" + key + "\":" + value.toString
    case (key, value: Any) => "\"" + key + "\":\"" + value.toString + "\""
  }.mkString("{", ",", "}")

  def mergeJson(json: String, jsonSeq: String*): String = {
    val initJson = json.trim match {
      case "" => ""
      case value => value.drop(1).dropRight(1)
    }

    "{" + jsonSeq.map(_.trim).fold(initJson) {
      case (j1, result) if j1.length < 5 => result.drop(1).dropRight(1)
      case (result, j2) if j2.length < 7 => result
      case (j1, j2) => j1 + "," + j2.drop(1).dropRight(1)
    } + "}"
  }

  private def parseKey(key: String): Array[Byte] = JwtBase64.decodeNonSafe(
    key.replaceAll("-----BEGIN (.*)-----", "")
     .replaceAll("-----END (.*)-----", "")
     .replaceAll("\r\n", "")
     .replaceAll("\n", "")
     .trim
  )

  private def parsePrivateKey(key: String, keyAlgo: String): PrivateKey = {
    val spec = new PKCS8EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(keyAlgo, PROVIDER).generatePrivate(spec)
  }

  private def parsePublicKey(key: String, keyAlgo: String): PublicKey = {
    val spec = new X509EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(keyAlgo, PROVIDER).generatePublic(spec)
  }

  private def signHmac(data: Array[Byte], key: String, algorithm: JwtAlgorithm): Array[Byte] = {
    val mac = Mac.getInstance(algorithm.fullName, PROVIDER)
    mac.init(new SecretKeySpec(bytify(key), algorithm.fullName))
    mac.doFinal(data)
  }

  private def signAsymetric(data: Array[Byte], key: String, algorithm: JwtAlgorithm, keyAlgo: String): Array[Byte] = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initSign(parsePrivateKey(key, keyAlgo))
    signer.update(data)
    signer.sign
  }

  private def signRSA(data: Array[Byte], key: String, algorithm: JwtAlgorithm): Array[Byte] =
    signAsymetric(data, key, algorithm, RSA)

  private def signECDSA(data: Array[Byte], key: String, algorithm: JwtAlgorithm): Array[Byte] =
    signAsymetric(data, key, algorithm, ECDSA)

  def sign(data: Array[Byte], key: Option[String], algorithm: Option[JwtAlgorithm]): Array[Byte] =
    (key, algorithm) match {
      case (Some(keyValue), Some(algoValue)) => {
        algoValue match {
          case _ : JwtHmacAlgorithm =>  signHmac(data, keyValue, algoValue)
          case _ : JwtRSAAlgorithm => signRSA(data, keyValue, algoValue)
          case _ : JwtECDSAAlgorithm => signECDSA(data, keyValue, algoValue)
        }
      }
      case _ => Array.empty[Byte]
    }

  def sign(data: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Array[Byte] =
    sign(bytify(data), key, algorithm)

  private def verifyHmac(data: Array[Byte], signature: Array[Byte], key: String, algorithm: JwtAlgorithm): Boolean =
    java.util.Arrays.equals(signHmac(data, key, algorithm), signature)

  private def verifyAsymetric(data: Array[Byte], signature: Array[Byte], key: String, algorithm: JwtAlgorithm, keyAlgo: String): Boolean = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initVerify(parsePublicKey(key, keyAlgo))
    signer.update(data)
    signer.verify(signature)
  }

  private def verifyRSA(data: Array[Byte], signature: Array[Byte], key: String, algorithm: JwtAlgorithm): Boolean =
    verifyAsymetric(data, signature, key, algorithm, RSA)

  private def verifyECDSA(data: Array[Byte], signature: Array[Byte], key: String, algorithm: JwtAlgorithm): Boolean =
    verifyAsymetric(data, signature, key, algorithm, ECDSA)

  def verify(data: Array[Byte], signature: Array[Byte], key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean = {
    (key, algorithm) match {
      case (Some(keyValue), Some(algoValue)) => {
        algoValue match {
          case _ : JwtHmacAlgorithm =>  verifyHmac(data, signature, keyValue, algoValue)
          case _ : JwtRSAAlgorithm => verifyRSA(data, signature, keyValue, algoValue)
          case _ : JwtECDSAAlgorithm => verifyECDSA(data, signature, keyValue, algoValue)
        }
      }
      case _ => signature.isEmpty
    }
  }

  def verify(data: String, signature: Array[Byte], key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(bytify(data), signature, key, algorithm)

  def verify(data: Array[Byte], signature: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(data, bytify(signature), key, algorithm)

  def verify(data: String, signature: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(bytify(data), bytify(signature), key, algorithm)
}
