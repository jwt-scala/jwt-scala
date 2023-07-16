package pdi.jwt

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey, Signature}
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Mac, SecretKey}

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.JwtNonSupportedAlgorithm

trait JwtUtilsPlatform { self: JwtUtils.type =>
  private def parseKey(key: String): Array[Byte] = JwtBase64.decodeNonSafe(
    key.replaceAll("-----BEGIN ([^-]*)-----|-----END ([^-]*)-----|\\s*", "")
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

}
