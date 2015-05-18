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

  /** Convert an array of bytes to its corresponding string using the default encoding.
    *
    * @return the final string
    * @param arr the array of bytes to transform
    */
  def stringify(arr: Array[Byte]): String = new String(arr, ENCODING)

  /** Convert a string to its corresponding array of bytes using the default encoding.
    *
    * @return the final array of bytes
    * @param str the string to convert
    */
  def bytify(str: String): Array[Byte] = str.getBytes(ENCODING)

  /** Convert a sequence to a JSON array
    */
  def seqToJson(seq: Seq[Any]): String = if (seq.isEmpty) {
    "[]"
  } else {
    seq.map {
      case value: String => "\"" + value + "\""
      case value: Boolean => (if (value) { "true" } else { "false" })
      case value: Double => value.toString
      case value: Short => value.toString
      case value: Float => value.toString
      case value: Long => value.toString
      case value: Int => value.toString
      case value: BigDecimal => value.toString
      case value: BigInt => value.toString
      case value: (String, Any) => hashToJson(Seq(value))
      case value: Any => "\"" + value.toString + "\""
    }.mkString("[", ",", "]")
  }

  /**
    * Convert a sequence of tuples to a JSON object
    */
  def hashToJson(hash: Seq[(String, Any)]): String = if (hash.isEmpty) {
    "{}"
  } else {
    hash.map {
      case (key, value: String) => "\"" + key + "\":\"" + value + "\""
      case (key, value: Boolean) => "\"" + key + "\":" + (if (value) { "true" } else { "false" })
      case (key, value: Double) => "\"" + key + "\":" + value.toString
      case (key, value: Short) => "\"" + key + "\":" + value.toString
      case (key, value: Float) => "\"" + key + "\":" + value.toString
      case (key, value: Long) => "\"" + key + "\":" + value.toString
      case (key, value: Int) => "\"" + key + "\":" + value.toString
      case (key, value: BigDecimal) => "\"" + key + "\":" + value.toString
      case (key, value: BigInt) => "\"" + key + "\":" + value.toString
      case (key, value: (String, Any)) => "\"" + key + "\":" + hashToJson(Seq(value))
      case (key, value: Seq[Any]) => "\"" + key + "\":" + seqToJson(value)
      case (key, value: Any) => "\"" + key + "\":\"" + value.toString + "\""
    }.mkString("{", ",", "}")
  }

  /**
    * Merge multiple JSON strings to a unique one
    */
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

  /**
    * Generate the signature for a given data using the key and algorithm provided. If none,
    * the signature will be empty.
    */
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

  /**
    * Alias to `sign` using a String data which will be converted to an array of bytes.
    */
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

  /**
    * Check if a signature is valid for a given data using the key and the algorithm provided.
    */
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

  /**
    * Alias for `verify`
    */
  def verify(data: String, signature: Array[Byte], key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(bytify(data), signature, key, algorithm)

  /**
    * Alias for `verify`
    */
  def verify(data: Array[Byte], signature: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(data, bytify(signature), key, algorithm)

  /**
    * Alias for `verify`
    */
  def verify(data: String, signature: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Boolean =
    verify(bytify(data), bytify(signature), key, algorithm)
}
