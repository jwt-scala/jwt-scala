package pdi.jwt

import javax.crypto.{Mac, SecretKey}
import javax.crypto.spec.SecretKeySpec
import java.security.{Security, Signature, KeyFactory, Key, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays

import pdi.jwt.algorithms._

object JwtUtils {
  // val ENCODING = "UTF-8"
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
  // def stringify(arr: Array[Byte]): String = new String(arr, ENCODING)

  /** Convert a string to its corresponding array of bytes using the default encoding.
    *
    * @return the final array of bytes
    * @param str the string to convert
    */
  // def bytify(str: String): Array[Byte] = str.getBytes(ENCODING)

  def parseKey(key: String): String =
    key.replaceAll("-----BEGIN (.*)-----", "")
     .replaceAll("-----END (.*)-----", "")
     .replaceAll("\r\n", "")
     .replaceAll("\n", "")
     .trim

  private def escape(value: String): String = value.replaceAll("\"", "\\\\\"")

  /** Convert a sequence to a JSON array
    */
  def seqToJson(seq: Seq[Any]): String = if (seq.isEmpty) {
    "[]"
  } else {
    seq.map {
      case value: String => "\"" + escape(value) + "\""
      case value: Boolean => (if (value) { "true" } else { "false" })
      case value: Double => value.toString
      case value: Short => value.toString
      case value: Float => value.toString
      case value: Long => value.toString
      case value: Int => value.toString
      case value: BigDecimal => value.toString
      case value: BigInt => value.toString
      case value: (String, Any) => hashToJson(Seq(value))
      case value: Any => "\"" + escape(value.toString) + "\""
    }.mkString("[", ",", "]")
  }

  /**
    * Convert a sequence of tuples to a JSON object
    */
  def hashToJson(hash: Seq[(String, Any)]): String = if (hash.isEmpty) {
    "{}"
  } else {
    hash.map {
      case (key, value: String) => "\"" + escape(key) + "\":\"" + escape(value) + "\""
      case (key, value: Boolean) => "\"" + escape(key) + "\":" + (if (value) { "true" } else { "false" })
      case (key, value: Double) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Short) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Float) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Long) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Int) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: BigDecimal) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: BigInt) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: (String, Any)) => "\"" + escape(key) + "\":" + hashToJson(Seq(value))
      case (key, value: Seq[Any]) => "\"" + escape(key) + "\":" + seqToJson(value)
      case (key, value: Set[Any]) => "\"" + escape(key) + "\":" + seqToJson(value.toSeq)
      case (key, value: Any) => "\"" + escape(key) + "\":\"" + escape(value.toString) + "\""
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

  private def parsePrivateKey(key: Array[Byte], keyAlgo: String): PrivateKey = {
    val spec = new PKCS8EncodedKeySpec(key)
    KeyFactory.getInstance(keyAlgo, PROVIDER).generatePrivate(spec)
  }

  private def parsePublicKey(key: Array[Byte], keyAlgo: String): PublicKey = {
    val spec = new X509EncodedKeySpec(key)
    KeyFactory.getInstance(keyAlgo, PROVIDER).generatePublic(spec)
  }

  /**
    * Generate the signature for a given data using the key and HMAC algorithm provided.
    */
  def sign(data: Array[Byte], key: SecretKey, algorithm: JwtHmacAlgorithm): Array[Byte] = {
    val mac = Mac.getInstance(algorithm.fullName, PROVIDER)
    mac.init(key)
    mac.doFinal(data)
  }

  def sign(data: String, key: SecretKey, algorithm: JwtHmacAlgorithm)(implicit ev: JwtArrayByteLike[String]): Array[Byte] =
    sign(JwtArrayByteLike.apply(data), key, algorithm)

  /**
    * Generate the signature for a given data using the key and RSA or ECDSA algorithm provided.
    */
  def sign(data: Array[Byte], key: PrivateKey, algorithm: JwtAsymetricAlgorithm): Array[Byte] = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initSign(key)
    signer.update(data)
    signer.sign
  }

  def sign(data: String, key: PrivateKey, algorithm: JwtAsymetricAlgorithm)(implicit ev: JwtArrayByteLike[String]): Array[Byte] =
    sign(JwtArrayByteLike.apply(data), key, algorithm)

  /**
    * Will try to sign some given data by parsing the provided key, if parsing fail, please consider retrieving the SecretKey or the PrivateKey on your side and then use another "sign" method.
    */
  def sign[T: JwtArrayByteLike](data: Array[Byte], key: T, algorithm: JwtAlgorithm): Array[Byte] =
    algorithm match {
      case algo: JwtHmacAlgorithm => sign(data, new SecretKeySpec(JwtArrayByteLike.apply(key), algo.fullName), algo)
      case algo: JwtRSAAlgorithm => sign(data, parsePrivateKey(JwtArrayByteLike.apply(key), RSA), algo)
      case algo: JwtECDSAAlgorithm => sign(data, parsePrivateKey(JwtArrayByteLike.apply(key), ECDSA), algo)
    }

  /**
    * Alias to `sign` using a String data which will be converted to an array of bytes.
    */
  def sign[T: JwtArrayByteLike](data: String, key: T, algorithm: JwtAlgorithm)(implicit ev: JwtArrayByteLike[String]): Array[Byte] =
    sign(JwtArrayByteLike.apply(data), JwtArrayByteLike.apply(key), algorithm)

  /**
    * Check if a signature is valid for a given data using the key and the HMAC algorithm provided.
    */
  def verify(data: Array[Byte], signature: Array[Byte], key: SecretKey, algorithm: JwtHmacAlgorithm): Boolean = {
    Arrays.constantTimeAreEqual(sign(data, key, algorithm), signature)
  }

  /**
    * Check if a signature is valid for a given data using the key and the RSA or ECDSA algorithm provided.
    */
  def verify(data: Array[Byte], signature: Array[Byte], key: PublicKey, algorithm: JwtAsymetricAlgorithm): Boolean = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initVerify(key)
    signer.update(data)
    signer.verify(signature)
  }

  /**
    * Will try to check if a signature is valid for a given data by parsing the provided key, if parsing fail, please consider retrieving the SecretKey or the PublicKey on your side and then use another "verify" method.
    */
  def verify[T: JwtArrayByteLike](data: Array[Byte], signature: Array[Byte], key: T, algorithm: JwtAlgorithm): Boolean =
    algorithm match {
      case algo: JwtHmacAlgorithm =>  verify(data, signature, new SecretKeySpec(JwtArrayByteLike.apply(key), algo.fullName), algo)
      case algo: JwtRSAAlgorithm => verify(data, signature, parsePublicKey(JwtArrayByteLike.apply(key), RSA), algo)
      case algo: JwtECDSAAlgorithm => verify(data, signature, parsePublicKey(JwtArrayByteLike.apply(key), ECDSA), algo)
    }

  /**
    * Alias for `verify`
    */
  def verify[T: JwtArrayByteLike](data: String, signature: String, key: T, algorithm: JwtAlgorithm)(implicit ev: JwtArrayByteLike[String]): Boolean =
    verify(JwtArrayByteLike.apply(data), JwtArrayByteLike.apply(signature), JwtArrayByteLike.apply(key), algorithm)
}
