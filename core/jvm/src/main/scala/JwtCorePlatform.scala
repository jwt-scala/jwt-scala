package pdi.jwt

import java.security.{Key, PrivateKey, PublicKey}
import javax.crypto.SecretKey
import scala.util.Try

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.*

trait JwtCorePlatform[H, C] extends JwtCoreFunctions { self: JwtCore[H, C] =>

  /** Encode a JSON Web Token from its different parts. Both the header and the claim will be
    * encoded to Base64 url-safe, then a signature will be eventually generated from it if you did
    * pass a key and an algorithm, and finally, those three parts will be merged as a single string,
    * using dots as separator.
    *
    * @return
    *   $token
    * @param header
    *   $headerString
    * @param claim
    *   $claimString
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(header: String, claim: String, key: String, algorithm: JwtAlgorithm): String = {
    val data = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim)
    data + "." + JwtBase64.encodeString(JwtUtils.sign(data, key, algorithm))
  }

  /** An alias to `encode` which will provide an automatically generated header and allowing you to
    * get rid of Option for the key and the algorithm.
    *
    * @return
    *   $token
    * @param claim
    *   $claimString
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: String, key: String, algorithm: JwtAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as
    * a case class.
    *
    * @return
    *   $token
    * @param claim
    *   the claim of the JSON Web Token
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: JwtClaim, key: String, algorithm: JwtAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be
    * deduced from the header.
    *
    * @return
    *   $token
    * @param header
    *   the header to stringify as a JSON before encoding the token
    * @param claim
    *   the claim to stringify as a JSON before encoding the token
    * @param key
    *   the secret key to use to sign the token (note that the algorithm will be deduced from the
    *   header)
    */
  def encode(header: JwtHeader, claim: JwtClaim, key: String): String = header.algorithm match {
    case Some(algo: JwtAlgorithm) => encode(header.toJson, claim.toJson, key, algo)
    case _                        => throw new JwtEmptyAlgorithmException()
  }

  def encode(header: String, claim: String, key: SecretKey, algorithm: JwtHmacAlgorithm): String = {
    val data = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim)
    data + "." + JwtBase64.encodeString(JwtUtils.sign(data, key, algorithm))
  }

  def encode(
      header: String,
      claim: String,
      key: PrivateKey,
      algorithm: JwtAsymmetricAlgorithm
  ): String = {
    val data = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim)
    data + "." + JwtBase64.encodeString(JwtUtils.sign(data, key, algorithm))
  }

  /** An alias to `encode` which will provide an automatically generated header and allowing you to
    * get rid of Option for the key and the algorithm.
    *
    * @return
    *   $token
    * @param claim
    *   $claimString
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: String, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and allowing you to
    * get rid of Option for the key and the algorithm.
    *
    * @return
    *   $token
    * @param claim
    *   $claimString
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: String, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as
    * a case class.
    *
    * @return
    *   $token
    * @param claim
    *   the claim of the JSON Web Token
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: JwtClaim, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as
    * a case class.
    *
    * @return
    *   $token
    * @param claim
    *   the claim of the JSON Web Token
    * @param key
    *   $key
    * @param algorithm
    *   $algo
    */
  def encode(claim: JwtClaim, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be
    * deduced from the header.
    *
    * @return
    *   $token
    * @param header
    *   the header to stringify as a JSON before encoding the token
    * @param claim
    *   the claim to stringify as a JSON before encoding the token
    * @param key
    *   the secret key to use to sign the token (note that the algorithm will be deduced from the
    *   header)
    */
  def encode(header: JwtHeader, claim: JwtClaim, key: Key): String = (header.algorithm, key) match {
    case (Some(algo: JwtHmacAlgorithm), k: SecretKey) =>
      encode(header.toJson, claim.toJson, k, algo)
    case (Some(algo: JwtAsymmetricAlgorithm), k: PrivateKey) =>
      encode(header.toJson, claim.toJson, k, algo)
    case _ =>
      throw new JwtValidationException(
        "The key type doesn't match the algorithm type. It's either a SecretKey and a HMAC algorithm or a PrivateKey and a RSA or ECDSA algorithm. And an algorithm is required of course."
      )
  }

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return
    *   if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRawAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
    (header, claim, signature)
  }

  def decodeRawAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using an asymmetric algorithm
    *
    * @return
    *   if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRawAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
    (header, claim, signature)
  }

  def decodeRawAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRaw(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRaw(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return
    *   if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRawAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
    (header, claim, signature)
  }

  def decodeRawAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeRawAll(
      token: String,
      key: SecretKey,
      options: JwtOptions
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtAlgorithm.allHmac(), options)

  def decodeRawAll(token: String, key: SecretKey): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using an asymmetric algorithm
    *
    * @return
    *   if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRawAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
    (header, claim, signature)
  }

  def decodeRawAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeRawAll(
      token: String,
      key: PublicKey,
      options: JwtOptions
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeRawAll(token: String, key: PublicKey): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRaw(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decodeRaw(token: String, key: SecretKey, options: JwtOptions): Try[String] =
    decodeRaw(token, key, JwtAlgorithm.allHmac(), options)

  def decodeRaw(token: String, key: SecretKey): Try[String] =
    decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeRaw(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decodeRaw(token: String, key: PublicKey, options: JwtOptions): Try[String] =
    decodeRaw(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeRaw(token: String, key: PublicKey): Try[String] =
    decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decodeAll(token: String, key: SecretKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allHmac(), options)

  def decodeAll(token: String, key: SecretKey): Try[(H, C, String)] =
    decodeAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decodeAll(token: String, key: PublicKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeAll(token: String, key: PublicKey): Try[(H, C, String)] =
    decodeAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decodeAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decode(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decode(token: String, key: SecretKey, options: JwtOptions): Try[C] =
    decode(token, key, JwtAlgorithm.allHmac(), options)

  def decode(token: String, key: SecretKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decode(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    */
  def decode(token: String, key: PublicKey, options: JwtOptions): Try[C] =
    decode(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decode(token: String, key: PublicKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decode(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def decode(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  // Validation for HMAC algorithm using a SecretKey
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Unit = validate(
    header64,
    header,
    claim64,
    claim,
    signature,
    options,
    (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) =>
      algorithm match {
        case algo: JwtHmacAlgorithm =>
          validateHmacAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
        case _ => false
      }
  )

  // Validation for RSA and ECDSA algorithms using PublicKey
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Unit = validate(
    header64,
    header,
    claim64,
    claim,
    signature,
    options,
    (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) =>
      algorithm match {
        case algo: JwtAsymmetricAlgorithm =>
          validateAsymmetricAlgorithm(algo, algorithms) && JwtUtils.verify(
            data,
            signature,
            key,
            algo
          )
        case _ => false
      }
  )

  /** An alias of `validate` in case you want to directly pass a string key.
    *
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    * @throws JwtValidationException
    *   default validation exception
    * @throws JwtLengthException
    *   the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException
    *   the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException
    *   the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException
    *   couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
  }

  def validate(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Unit =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: SecretKey, options: JwtOptions): Unit =
    validate(token, key, JwtAlgorithm.allHmac(), options)

  def validate(token: String, key: SecretKey): Unit = validate(token, key, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key.
    *
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    * @throws JwtValidationException
    *   default validation exception
    * @throws JwtLengthException
    *   the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException
    *   the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException
    *   the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException
    *   couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
  }

  def validate(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Unit =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: PublicKey, options: JwtOptions): Unit =
    validate(token, key, JwtAlgorithm.allAsymmetric(), options)

  def validate(token: String, key: PublicKey): Unit = validate(token, key, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key for HMAC algorithms.
    *
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    * @throws JwtValidationException
    *   default validation exception
    * @throws JwtLengthException
    *   the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException
    *   the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException
    *   the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException
    *   couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
  }

  def validate(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Unit =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key for asymmetric
    * algorithms.
    *
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    * @throws JwtValidationException
    *   default validation exception
    * @throws JwtLengthException
    *   the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException
    *   the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException
    *   the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException
    *   couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(
      header64,
      parseHeader(header),
      claim64,
      parseClaim(claim),
      signature,
      key,
      algorithms,
      options
    )
  }

  def validate(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Unit =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return
    *   a boolean value indicating if the token is valid or not
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def isValid(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Boolean = Try(validate(token, key, algorithms, options)).isSuccess

  def isValid(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: SecretKey, options: JwtOptions): Boolean =
    isValid(token, key, JwtAlgorithm.allHmac(), options)

  def isValid(token: String, key: SecretKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return
    *   a boolean value indicating if the token is valid or not
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def isValid(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Boolean = Try(validate(token, key, algorithms, options)).isSuccess

  def isValid(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: PublicKey, options: JwtOptions): Boolean =
    isValid(token, key, JwtAlgorithm.allAsymmetric(), options)

  def isValid(token: String, key: PublicKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for HMAC algorithms
    *
    * @return
    *   a boolean value indicating if the token is valid or not
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def isValid(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Boolean = Try(validate(token, key, algorithms, options)).isSuccess

  def isValid(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for asymmetric
    * algorithms
    *
    * @return
    *   a boolean value indicating if the token is valid or not
    * @param token
    *   $token
    * @param key
    *   $key
    * @param algorithms
    *   $algos
    */
  def isValid(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Boolean = Try(validate(token, key, algorithms, options)).isSuccess

  def isValid(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  // Generic validation on String Key for HMAC algorithms
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Unit = validate(
    header64,
    header,
    claim64,
    claim,
    signature,
    options,
    (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) =>
      algorithm match {
        case algo: JwtHmacAlgorithm =>
          validateHmacAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
        case _ => false
      }
  )

  // Generic validation on String Key for asymmetric algorithms
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Unit = validate(
    header64,
    header,
    claim64,
    claim,
    signature,
    options,
    (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) =>
      algorithm match {
        case algo: JwtAsymmetricAlgorithm =>
          validateAsymmetricAlgorithm(algo, algorithms) && JwtUtils.verify(
            data,
            signature,
            key,
            algo
          )
        case _ => false
      }
  )
}
