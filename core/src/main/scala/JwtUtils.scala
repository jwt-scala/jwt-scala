package pdi.jwt

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey, Signature}
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Mac, SecretKey}

import pdi.jwt.JwtAlgorithm.{ES256, ES384, ES512}
import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.{JwtNonSupportedAlgorithm, JwtSignatureFormatException}

object JwtUtils {
  val ENCODING = "UTF-8"
  val RSA = "RSA"
  val ECDSA = "EC"
  val EdDSA = "EdDSA"

  /** Convert an array of bytes to its corresponding string using the default encoding.
    *
    * @return
    *   the final string
    * @param arr
    *   the array of bytes to transform
    */
  def stringify(arr: Array[Byte]): String = new String(arr, ENCODING)

  /** Convert a string to its corresponding array of bytes using the default encoding.
    *
    * @return
    *   the final array of bytes
    * @param str
    *   the string to convert
    */
  def bytify(str: String): Array[Byte] = str.getBytes(ENCODING)

  private def escape(value: String): String = value.replaceAll("\"", "\\\\\"")

  /** Convert a sequence to a JSON array
    */
  def seqToJson(seq: Seq[Any]): String = seq
    .map {
      case value: String        => "\"" + escape(value) + "\""
      case value: Boolean       => if (value) "true" else "false"
      case value: Double        => value.toString
      case value: Short         => value.toString
      case value: Float         => value.toString
      case value: Long          => value.toString
      case value: Int           => value.toString
      case value: BigDecimal    => value.toString
      case value: BigInt        => value.toString
      case (key: String, value) => hashToJson(Seq(key -> value))
      case value: Any           => "\"" + escape(value.toString) + "\""
    }
    .mkString("[", ",", "]")

  /** Convert a sequence of tuples to a JSON object
    */
  def hashToJson(hash: Seq[(String, Any)]): String = hash
    .map {
      case (key, value: String)     => "\"" + escape(key) + "\":\"" + escape(value) + "\""
      case (key, value: Boolean)    => "\"" + escape(key) + "\":" + (if (value) "true" else "false")
      case (key, value: Double)     => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Short)      => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Float)      => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Long)       => "\"" + escape(key) + "\":" + value.toString
      case (key, value: Int)        => "\"" + escape(key) + "\":" + value.toString
      case (key, value: BigDecimal) => "\"" + escape(key) + "\":" + value.toString
      case (key, value: BigInt)     => "\"" + escape(key) + "\":" + value.toString
      case (key, (vKey: String, vValue)) =>
        "\"" + escape(key) + "\":" + hashToJson(Seq(vKey -> vValue))
      case (key, value: Seq[Any]) => "\"" + escape(key) + "\":" + seqToJson(value)
      case (key, value: Set[_])   => "\"" + escape(key) + "\":" + seqToJson(value.toSeq)
      case (key, value: Any)      => "\"" + escape(key) + "\":\"" + escape(value.toString) + "\""
    }
    .mkString("{", ",", "}")

  /** Merge multiple JSON strings to a unique one
    */
  def mergeJson(json: String, jsonSeq: String*): String = {
    val initJson = json.trim match {
      case ""    => ""
      case value => value.drop(1).dropRight(1)
    }

    "{" + jsonSeq.map(_.trim).fold(initJson) {
      case (j1, result) if j1.length < 5 => result.drop(1).dropRight(1)
      case (result, j2) if j2.length < 7 => result
      case (j1, j2)                      => j1 + "," + j2.drop(1).dropRight(1)
    } + "}"
  }

  private def parseKey(key: String): Array[Byte] = JwtBase64.decodeNonSafe(
    key
      .replaceAll("-----BEGIN (.*)-----", "")
      .replaceAll("-----END (.*)-----", "")
      .replaceAll("\r\n", "")
      .replaceAll("\n", "")
      .trim
  )

  private def parsePrivateKey(key: String, keyAlgo: String) = {
    val spec = new PKCS8EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(keyAlgo).generatePrivate(spec)
  }

  private def parsePublicKey(key: String, keyAlgo: String): PublicKey = {
    val spec = new X509EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(keyAlgo).generatePublic(spec)
  }

  /** Generate the signature for a given data using the key and HMAC algorithm provided.
    */
  def sign(data: Array[Byte], key: SecretKey, algorithm: JwtHmacAlgorithm): Array[Byte] = {
    val mac = Mac.getInstance(algorithm.fullName)
    mac.init(key)
    mac.doFinal(data)
  }

  def sign(data: String, key: SecretKey, algorithm: JwtHmacAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /** Generate the signature for a given data using the key and RSA or ECDSA algorithm provided.
    */
  def sign(data: Array[Byte], key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): Array[Byte] = {
    val signer = Signature.getInstance(algorithm.fullName)
    signer.initSign(key)
    signer.update(data)
    algorithm match {
      case _: JwtRSAAlgorithm => signer.sign
      case algorithm: JwtECDSAAlgorithm =>
        transcodeSignatureToConcat(signer.sign, getSignatureByteArrayLength(algorithm))
      case _: JwtEdDSAAlgorithm => signer.sign
    }
  }

  def sign(data: String, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /** Will try to sign some given data by parsing the provided key, if parsing fail, please consider
    * retrieving the SecretKey or the PrivateKey on your side and then use another "sign" method.
    */
  def sign(data: Array[Byte], key: String, algorithm: JwtAlgorithm): Array[Byte] =
    algorithm match {
      case algo: JwtHmacAlgorithm => sign(data, new SecretKeySpec(bytify(key), algo.fullName), algo)
      case algo: JwtRSAAlgorithm  => sign(data, parsePrivateKey(key, RSA), algo)
      case algo: JwtECDSAAlgorithm   => sign(data, parsePrivateKey(key, ECDSA), algo)
      case algo: JwtEdDSAAlgorithm   => sign(data, parsePrivateKey(key, EdDSA), algo)
      case algo: JwtUnknownAlgorithm => throw new JwtNonSupportedAlgorithm(algo.fullName)
    }

  /** Alias to `sign` using a String data which will be converted to an array of bytes.
    */
  def sign(data: String, key: String, algorithm: JwtAlgorithm): Array[Byte] =
    sign(bytify(data), key, algorithm)

  /** Check if a signature is valid for a given data using the key and the HMAC algorithm provided.
    */
  def verify(
      data: Array[Byte],
      signature: Array[Byte],
      key: SecretKey,
      algorithm: JwtHmacAlgorithm
  ): Boolean = {
    JwtArrayUtils.constantTimeAreEqual(sign(data, key, algorithm), signature)
  }

  /** Check if a signature is valid for a given data using the key and the RSA or ECDSA algorithm
    * provided.
    */
  def verify(
      data: Array[Byte],
      signature: Array[Byte],
      key: PublicKey,
      algorithm: JwtAsymmetricAlgorithm
  ): Boolean = {
    val signer = Signature.getInstance(algorithm.fullName)
    signer.initVerify(key)
    signer.update(data)
    algorithm match {
      case _: JwtRSAAlgorithm   => signer.verify(signature)
      case _: JwtECDSAAlgorithm => signer.verify(transcodeSignatureToDER(signature))
      case _: JwtEdDSAAlgorithm => signer.verify(signature)
    }
  }

  /** Will try to check if a signature is valid for a given data by parsing the provided key, if
    * parsing fail, please consider retrieving the SecretKey or the PublicKey on your side and then
    * use another "verify" method.
    */
  def verify(
      data: Array[Byte],
      signature: Array[Byte],
      key: String,
      algorithm: JwtAlgorithm
  ): Boolean = algorithm match {
    case algo: JwtHmacAlgorithm =>
      verify(data, signature, new SecretKeySpec(bytify(key), algo.fullName), algo)
    case algo: JwtRSAAlgorithm     => verify(data, signature, parsePublicKey(key, RSA), algo)
    case algo: JwtECDSAAlgorithm   => verify(data, signature, parsePublicKey(key, ECDSA), algo)
    case algo: JwtEdDSAAlgorithm   => verify(data, signature, parsePublicKey(key, EdDSA), algo)
    case algo: JwtUnknownAlgorithm => throw new JwtNonSupportedAlgorithm(algo.fullName)
  }

  /** Alias for `verify`
    */
  def verify(data: String, signature: String, key: String, algorithm: JwtAlgorithm): Boolean =
    verify(bytify(data), bytify(signature), key, algorithm)

  /** Returns the expected signature byte array length (R + S parts) for the specified ECDSA
    * algorithm.
    *
    * @param algorithm
    *   The ECDSA algorithm. Must be supported and not { @code null}.
    * @return
    *   The expected byte array length for the signature.
    */
  def getSignatureByteArrayLength(algorithm: JwtECDSAAlgorithm): Int = algorithm match {
    case ES256 => 64
    case ES384 => 96
    case ES512 => 132
  }

  /** Transcodes the JCA ASN.1/DER-encoded signature into the concatenated R + S format expected by
    * ECDSA JWS.
    *
    * @param derSignature
    *   The ASN1./DER-encoded. Must not be { @code null}.
    * @param outputLength
    *   The expected length of the ECDSA JWS signature.
    * @return
    *   The ECDSA JWS encoded signature.
    * @throws JwtSignatureFormatException
    *   If the ASN.1/DER signature format is invalid.
    */
  @throws[JwtSignatureFormatException]
  def transcodeSignatureToConcat(derSignature: Array[Byte], outputLength: Int): Array[Byte] = {
    if (derSignature.length < 8 || derSignature(0) != 48)
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    val offset: Int = derSignature(1) match {
      case s if s > 0            => 2
      case s if s == 0x81.toByte => 3
      case _ => throw new JwtSignatureFormatException("Invalid ECDSA signature format")
    }

    val rLength: Byte = derSignature(offset + 1)
    var i = rLength.toInt
    while ((i > 0) && (derSignature((offset + 2 + rLength) - i) == 0)) {
      i -= 1
    }

    val sLength: Byte = derSignature(offset + 2 + rLength + 1)
    var j = sLength.toInt
    while ((j > 0) && (derSignature((offset + 2 + rLength + 2 + sLength) - j) == 0)) {
      j -= 1
    }

    val rawLen: Int = Math.max(Math.max(i, j), outputLength / 2)

    if (
      (derSignature(offset - 1) & 0xff) != derSignature.length - offset
      || (derSignature(offset - 1) & 0xff) != 2 + rLength + 2 + sLength
      || derSignature(offset) != 2 || derSignature(offset + 2 + rLength) != 2
    )
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    val concatSignature: Array[Byte] = new Array[Byte](2 * rawLen)
    System.arraycopy(derSignature, (offset + 2 + rLength) - i, concatSignature, rawLen - i, i)
    System.arraycopy(
      derSignature,
      (offset + 2 + rLength + 2 + sLength) - j,
      concatSignature,
      2 * rawLen - j,
      j
    )
    concatSignature
  }

  /** Transcodes the ECDSA JWS signature into ASN.1/DER format for use by the JCA verifier.
    *
    * @param signature
    *   The JWS signature, consisting of the concatenated R and S values. Must not be { @code null}.
    * @return
    *   The ASN.1/DER encoded signature.
    * @throws JwtSignatureFormatException
    *   If the ECDSA JWS signature format is invalid.
    */
  @throws[JwtSignatureFormatException]
  def transcodeSignatureToDER(signature: Array[Byte]): Array[Byte] = {
    var (r, s) = signature.splitAt(signature.length / 2)
    r = r.dropWhile(_ == 0)
    if (r.length > 0 && r(0) < 0)
      r +:= 0.toByte

    s = s.dropWhile(_ == 0)
    if (s.length > 0 && s(0) < 0)
      s +:= 0.toByte

    val signatureLength = 2 + r.length + 2 + s.length

    if (signatureLength > 255)
      throw new JwtSignatureFormatException("Invalid ECDSA signature format")

    val signatureDER = scala.collection.mutable.ListBuffer.empty[Byte]
    signatureDER += 48
    if (signatureLength >= 128)
      signatureDER += 0x81.toByte

    signatureDER += signatureLength.toByte
    signatureDER += 2.toByte += r.length.toByte ++= r
    signatureDER += 2.toByte += s.length.toByte ++= s

    signatureDER.toArray
  }

  def splitString(input: String, separator: Char): Array[String] = {
    val builder = scala.collection.mutable.ArrayBuffer.empty[String]
    var lastIndex = 0
    var index = input.indexOf(separator.toInt, lastIndex)
    while (index != -1) {
      builder += input.substring(lastIndex, index)
      lastIndex = index + 1
      index = input.indexOf(separator.toInt, lastIndex)
    }
    // Add the remainder
    if (lastIndex < input.length) {
      builder += input.substring(lastIndex, input.length)
    }
    builder.toArray
  }

}
