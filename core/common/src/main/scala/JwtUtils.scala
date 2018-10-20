package pdi.jwt

import javax.crypto.{Mac, SecretKey}
import javax.crypto.spec.SecretKeySpec
import java.security.{ KeyFactory, PrivateKey, PublicKey, Security, Signature}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Arrays
import pdi.jwt.JwtAlgorithm.{ES256, ES384, ES512}
import pdi.jwt.algorithms._
import pdi.jwt.exceptions.{JwtNonSupportedAlgorithm, JwtSignatureFormatException}

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

  /**
    * Generate the signature for a given data using the key and HMAC algorithm provided.
    */
  def sign(data: Array[Byte], key: SecretKey, algorithm: JwtHmacAlgorithm): Array[Byte] = {
    val mac = Mac.getInstance(algorithm.fullName, PROVIDER)
    mac.init(key)
    mac.doFinal(data)
  }

  def sign(data: String, key: SecretKey, algorithm: JwtHmacAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /**
    * Generate the signature for a given data using the key and RSA or ECDSA algorithm provided.
    */
  def sign(data: Array[Byte], key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): Array[Byte] = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initSign(key)
    signer.update(data)
    algorithm match {
      case algorithm: JwtRSAAlgorithm => signer.sign
      case algorithm: JwtECDSAAlgorithm => transcodeSignatureToConcat(signer.sign, getSignatureByteArrayLength(algorithm))
    }
  }

  def sign(data: String, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /**
    * Will try to sign some given data by parsing the provided key, if parsing fail, please consider retrieving the SecretKey or the PrivateKey on your side and then use another "sign" method.
    */
  def sign(data: Array[Byte], key: String, algorithm: JwtAlgorithm): Array[Byte] =
    algorithm match {
      case algo: JwtHmacAlgorithm => sign(data, new SecretKeySpec(bytify(key), algo.fullName), algo)
      case algo: JwtRSAAlgorithm => sign(data, parsePrivateKey(key, RSA), algo)
      case algo: JwtECDSAAlgorithm => sign(data, parsePrivateKey(key, ECDSA), algo)
    }

  /**
    * Alias to `sign` using a String data which will be converted to an array of bytes.
    */
  def sign(data: String, key: String, algorithm: JwtAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /**
    * Check if a signature is valid for a given data using the key and the HMAC algorithm provided.
    */
  def verify(data: Array[Byte], signature: Array[Byte], key: SecretKey, algorithm: JwtHmacAlgorithm): Boolean = {
    Arrays.constantTimeAreEqual(sign(data, key, algorithm), signature)
  }

  /**
    * Check if a signature is valid for a given data using the key and the RSA or ECDSA algorithm provided.
    */
  def verify(data: Array[Byte], signature: Array[Byte], key: PublicKey, algorithm: JwtAsymmetricAlgorithm): Boolean = {
    val signer = Signature.getInstance(algorithm.fullName, PROVIDER)
    signer.initVerify(key)
    signer.update(data)
    algorithm match {
      case algo: JwtRSAAlgorithm => signer.verify(signature)
      case algo: JwtECDSAAlgorithm => signer.verify(transcodeSignatureToDER(signature))
    }
  }

  /**
    * Will try to check if a signature is valid for a given data by parsing the provided key, if parsing fail, please consider retrieving the SecretKey or the PublicKey on your side and then use another "verify" method.
    */
  def verify(data: Array[Byte], signature: Array[Byte], key: String, algorithm: JwtAlgorithm): Boolean =
    algorithm match {
      case algo: JwtHmacAlgorithm =>  verify(data, signature, new SecretKeySpec(bytify(key), algo.fullName), algo)
      case algo: JwtRSAAlgorithm => verify(data, signature, parsePublicKey(key, RSA), algo)
      case algo: JwtECDSAAlgorithm => verify(data, signature, parsePublicKey(key, ECDSA), algo)
    }

  /**
    * Alias for `verify`
    */
  def verify(data: String, signature: String, key: String, algorithm: JwtAlgorithm): Boolean =
    verify(bytify(data), bytify(signature), key, algorithm)

  /**
    * Returns the expected signature byte array length (R + S parts) for
    * the specified ECDSA algorithm.
    *
    * @param algorithm The ECDSA algorithm. Must be supported and not { @code null}.
    * @return The expected byte array length for the signature.
    * @throws JwtNonSupportedAlgorithm If the algorithm is not supported.
    */
  @throws[JwtNonSupportedAlgorithm]
  def getSignatureByteArrayLength(algorithm: JwtECDSAAlgorithm): Int = {
    algorithm match {
      case ES256 => 64
      case ES384 => 96
      case ES512 => 132
    }
  }

  /**
    * Transcodes the JCA ASN.1/DER-encoded signature into the concatenated
    * R + S format expected by ECDSA JWS.
    *
    * @param derSignature The ASN1./DER-encoded. Must not be { @code null}.
    * @param outputLength The expected length of the ECDSA JWS signature.
    * @return The ECDSA JWS encoded signature.
    * @throws JwtSignatureFormatException If the ASN.1/DER signature format is invalid.
    */
  @throws[JwtSignatureFormatException]
  def transcodeSignatureToConcat(derSignature: Array[Byte], outputLength: Int): Array[Byte] = {
    if (derSignature.length < 8 || derSignature(0) != 48)
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    var offset: Int = 0
    if (derSignature(1) > 0) offset = 2
    else if (derSignature(1) == 0x81.toByte) offset = 3
    else throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    val rLength: Byte = derSignature(offset + 1)
    var i: Int = rLength
    while ((i > 0) && (derSignature((offset + 2 + rLength) - i) == 0)) {
      i -= 1
    }

    val sLength: Byte = derSignature(offset + 2 + rLength + 1)
    var j: Int = sLength
    while ((j > 0) && (derSignature((offset + 2 + rLength + 2 + sLength) - j) == 0)) {
      j -= 1
    }

    var rawLen: Int = Math.max(i, j)
    rawLen = Math.max(rawLen, outputLength / 2)

    if ((derSignature(offset - 1) & 0xff) != derSignature.length - offset
      || (derSignature(offset - 1) & 0xff) != 2 + rLength + 2 + sLength
      || derSignature(offset) != 2 || derSignature(offset + 2 + rLength) != 2)
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    val concatSignature: Array[Byte] = new Array[Byte](2 * rawLen)
    System.arraycopy(derSignature, (offset + 2 + rLength) - i, concatSignature, rawLen - i, i)
    System.arraycopy(derSignature, (offset + 2 + rLength + 2 + sLength) - j, concatSignature, 2 * rawLen - j, j)
    concatSignature
  }

  /**
    * Transcodes the ECDSA JWS signature into ASN.1/DER format for use by
    * the JCA verifier.
    *
    * @param signature The JWS signature, consisting of the
    *                     concatenated R and S values. Must not be
    *                     { @code null}.
    * @return The ASN.1/DER encoded signature.
    * @throws JwtSignatureFormatException If the ECDSA JWS signature format is invalid.
    */
  @throws[JwtSignatureFormatException]
  def transcodeSignatureToDER(signature: Array[Byte]): Array[Byte] = {
    var (r,s) = signature.splitAt(signature.length / 2)
    r = r.dropWhile(_ == 0)
    if (r.length > 0 && r(0) < 0)
      r +:= 0.toByte

    s = s.dropWhile(_ == 0)
    if (s.length > 0 && s(0) < 0)
      s +:= 0.toByte

    val signatureLength = 2 + r.length + 2 + s.length

    if (signatureLength > 255)
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    var signatureDER = scala.collection.mutable.ListBuffer.empty[Byte]
    signatureDER += 48
    if (signatureLength >= 128)
      signatureDER += 0x81.toByte

    signatureDER += signatureLength.toByte
    signatureDER += 2.toByte += r.length.toByte ++= r
    signatureDER += 2.toByte += s.length.toByte ++= s

    signatureDER.toArray
  }

}
