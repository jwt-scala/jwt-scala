package pdi.jwt

import scala.util.Try
import javax.crypto.SecretKey
import java.security.{Key, PrivateKey, PublicKey}
import java.time.Clock

import pdi.jwt.algorithms._
import pdi.jwt.exceptions._
import scala.util.Success
import scala.util.Failure

/** Provide the main logic around Base64 encoding / decoding and signature using the correct algorithm.
  * '''H''' and '''C''' types are respesctively the header type and the claim type. For the core project,
  * they will be String but you are free to extend this trait using other types like
  * JsObject or anything else.
  *
  * Please, check implementations, like [[Jwt]], for code samples.
  *
  * @tparam H the type of the extracted header from a JSON Web Token
  * @tparam C the type of the extracted claim from a JSON Web Token
  *
  * @define token a JSON Web Token as a Base64 url-safe encoded String which can be used inside an HTTP header
  * @define headerString a valid stringified JSON representing the header of the token
  * @define claimString a valid stringified JSON representing the claim of the token
  * @define key the key that will be used to check the token signature
  * @define algo the algorithm to sign the token
  * @define algos a list of possible algorithms that the token can use. See [[https://jwt-scala.github.io/jwt-scala/#security-concerns Security concerns]] for more infos.
  */
trait JwtCore[H, C] {
  implicit private[jwt] val clock: Clock = Clock.systemUTC
  // Abstract methods
  protected def parseHeader(header: String): Try[H]
  protected def parseClaim(claim: String): Try[C]

  protected def extractAlgorithm(header: H): Option[JwtAlgorithm]
  protected def extractExpiration(claim: C): Option[Long]
  protected def extractNotBefore(claim: C): Option[Long]

  def encode(header: String, claim: String): String = {
    JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim) + "."
  }

  /** Encode a JSON Web Token from its different parts. Both the header and the claim will be encoded to Base64 url-safe, then a signature will be eventually generated from it if you did pass a key and an algorithm, and finally, those three parts will be merged as a single string, using dots as separator.
    *
    * @return $token
    * @param header $headerString
    * @param claim $claimString
    * @param key $key
    * @param algorithm $algo
    */
  def encode(header: String, claim: String, key: String, algorithm: JwtAlgorithm): String = {
    val data = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim)
    data + "." + JwtBase64.encodeString(JwtUtils.sign(data, key, algorithm))
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

  /** An alias to `encode` which will provide an automatically generated header.
    *
    * @return $token
    * @param claim $claimString
    */
  def encode(claim: String): String = encode(JwtHeader().toJson, claim)

  /** An alias to `encode` which will provide an automatically generated header and allowing you to get rid of Option
    * for the key and the algorithm.
    *
    * @return $token
    * @param claim $claimString
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: String, key: String, algorithm: JwtAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and allowing you to get rid of Option
    * for the key and the algorithm.
    *
    * @return $token
    * @param claim $claimString
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: String, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and allowing you to get rid of Option
    * for the key and the algorithm.
    *
    * @return $token
    * @param claim $claimString
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: String, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String =
    encode(JwtHeader(algorithm).toJson, claim, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and setting both key and algorithm
    * to None.
    *
    * @return $token
    * @param claim the claim of the JSON Web Token
    */
  def encode(claim: JwtClaim): String = encode(claim.toJson)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as a case class.
    *
    * @return $token
    * @param claim the claim of the JSON Web Token
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: JwtClaim, key: String, algorithm: JwtAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as a case class.
    *
    * @return $token
    * @param claim the claim of the JSON Web Token
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: JwtClaim, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias to `encode` which will provide an automatically generated header and use the claim as a case class.
    *
    * @return $token
    * @param claim the claim of the JSON Web Token
    * @param key $key
    * @param algorithm $algo
    */
  def encode(claim: JwtClaim, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String =
    encode(claim.toJson, key, algorithm)

  /** An alias to `encode` if you want to use case classes for the header and the claim rather than strings, they will just be stringified to JSON format.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    */
  def encode(header: JwtHeader, claim: JwtClaim): String = header.algorithm match {
    case None => encode(header.toJson, claim.toJson)
    case _    => throw new JwtNonEmptyAlgorithmException()
  }

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be deduced from the header.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    * @param key the secret key to use to sign the token (note that the algorithm will be deduced from the header)
    */
  def encode(header: JwtHeader, claim: JwtClaim, key: String): String = header.algorithm match {
    case Some(algo: JwtAlgorithm) => encode(header.toJson, claim.toJson, key, algo)
    case _                        => throw new JwtEmptyAlgorithmException()
  }

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be deduced from the header.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    * @param key the secret key to use to sign the token (note that the algorithm will be deduced from the header)
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

  /** @return a tuple of (header64, header, claim64, claim, signature or empty string if none)
    * @throws JwtLengthException if there is not 2 or 3 parts in the token
    */
  private def splitToken(token: String): Try[(String, String, String, String, String)] = {
    val parts = JwtUtils.splitString(token, '.')

    for {
      signature <- parts.length match {
        case 2 => Success("")
        case 3 => Success(parts(2))
        case _ =>
          Failure(
            new JwtLengthException(
              s"Expected token [$token] to be composed of 2 or 3 parts separated by dots."
            )
          )
      }
      header64 = parts(0)
      header <- Try(JwtBase64.decodeString(header64))
      claim64 = parts(1)
      claim <- Try(JwtBase64.decodeString(claim64))
    } yield (header64, header, claim64, claim, signature)
  }

  /** Will try to decode a JSON Web Token to raw strings
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    */
  def decodeRawAll(token: String, options: JwtOptions): Try[(String, String, String)] = for {
    (_, header, _, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(h, c, signature, options)
  } yield (header, claim, signature)

  def decodeRawAll(token: String): Try[(String, String, String)] =
    decodeRawAll(token, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (header, claim, signature)

  def decodeRawAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using an asymmetric algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (header, claim, signature)

  def decodeRawAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (header, claim, signature)

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
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(String, String, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (header, claim, signature)

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

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    */
  def decodeRaw(token: String, options: JwtOptions): Try[String] =
    decodeRawAll(token, options).map(_._2)

  def decodeRaw(token: String): Try[String] = decodeRaw(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    */
  def decodeRaw(token: String, key: SecretKey, options: JwtOptions): Try[String] =
    decodeRaw(token, key, JwtAlgorithm.allHmac(), options)

  def decodeRaw(token: String, key: SecretKey): Try[String] =
    decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    */
  def decodeRaw(token: String, key: PublicKey, options: JwtOptions): Try[String] =
    decodeRaw(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeRaw(token: String, key: PublicKey): Try[String] =
    decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    */
  def decodeAll(token: String, options: JwtOptions): Try[(H, C, String)] = for {
    (_, header, _, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(h, c, signature, options)
  } yield (h, c, signature)

  def decodeAll(token: String): Try[(H, C, String)] = decodeAll(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (h, c, signature)

  def decodeAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (h, c, signature)

  def decodeAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (h, c, signature)

  def decodeAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    */
  def decodeAll(token: String, key: SecretKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allHmac(), options)

  def decodeAll(token: String, key: SecretKey): Try[(H, C, String)] =
    decodeAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(H, C, String)] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield (h, c, signature)

  def decodeAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    */
  def decodeAll(token: String, key: PublicKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeAll(token: String, key: PublicKey): Try[(H, C, String)] =
    decodeAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    */
  def decode(token: String, options: JwtOptions): Try[C] = decodeAll(token, options).map(_._2)

  def decode(token: String): Try[C] = decode(token, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    */
  def decode(token: String, key: SecretKey, options: JwtOptions): Try[C] =
    decode(token, key, JwtAlgorithm.allHmac(), options)

  def decode(token: String, key: SecretKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    * @param algorithms $algos
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
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    */
  def decode(token: String, key: PublicKey, options: JwtOptions): Try[C] =
    decode(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decode(token: String, key: PublicKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  // Validate
  protected def validateTiming(claim: C, options: JwtOptions) = {
    val maybeExpiration: Option[Long] =
      if (options.expiration) extractExpiration(claim) else None

    val maybeNotBefore: Option[Long] =
      if (options.notBefore) extractNotBefore(claim) else None

    JwtTime.validateNowIsBetweenSeconds(
      maybeNotBefore.map(_ - options.leeway),
      maybeExpiration.map(_ + options.leeway)
    )
  }

  // Validate if an algorithm is inside the authorized range
  protected def validateHmacAlgorithm(
      algorithm: JwtHmacAlgorithm,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Boolean = {
    algorithms.contains(algorithm)
  }

  // Validate if an algorithm is inside the authorized range
  protected def validateAsymmetricAlgorithm(
      algorithm: JwtAsymmetricAlgorithm,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Boolean = {
    algorithms.contains(algorithm)
  }

  // Validation when no key and no algorithm (or unknown)
  protected def validate(header: H, claim: C, signature: String, options: JwtOptions): Try[Unit] =
    for {
      _ <-
        if (options.signature) {
          if (!signature.isEmpty) {
            Failure(new JwtNonEmptySignatureException())
          } else {
            extractAlgorithm(header) match {
              case Some(JwtUnknownAlgorithm(name)) => Failure(new JwtNonSupportedAlgorithm(name))
              case Some(_)                         => Failure(new JwtNonEmptyAlgorithmException())
              case None                            => Success(())
            }
          }
        } else Success(())
      _ <- validateTiming(claim, options)
    } yield ()

  // Validation when both key and algorithm
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      options: JwtOptions,
      verify: (Array[Byte], Array[Byte], JwtAlgorithm) => Boolean
  ): Try[Unit] = for {
    _ <-
      if (options.signature) {
        if (signature.isEmpty) {
          Failure(new JwtNonEmptySignatureException())
        } else {
          extractAlgorithm(header) match {
            case None => Failure(new JwtEmptyAlgorithmException())
            case Some(algo)
                if !verify(
                  JwtUtils.bytify(header64 + "." + claim64),
                  JwtBase64.decode(signature),
                  algo
                ) =>
              Failure(
                new JwtValidationException("Invalid signature for this token or wrong algorithm.")
              )
            case _ => Success(())
          }
        }
      } else Success(())
    _ <- validateTiming(claim, options)
  } yield ()

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
  ): Try[Unit] = {
    validate(
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
  }

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
  ): Try[Unit] = {
    validate(
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
  ): Try[Unit] = {
    validate(
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
  }

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
  ): Try[Unit] = {
    validate(
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

  /** Valid a token: doesn't return anything but will thrown exceptions if there are any errors.
    *
    * @param token $token
    * @throws JwtValidationException default validation exception
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException couldn't decode the token since it's not a valid base64 string
    */
  def validate(token: String, options: JwtOptions): Try[Unit] = for {
    (_, header, _, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(h, c, signature, options)
  } yield ()

  def validate(token: String): Try[Unit] = validate(token, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key for HMAC algorithms.
    *
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    * @throws JwtValidationException default validation exception
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[Unit] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield ()

  def validate(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[Unit] =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key for asymmetric algorithms.
    *
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    * @throws JwtValidationException default validation exception
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[Unit] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield ()

  def validate(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[Unit] =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key.
    *
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    * @throws JwtValidationException default validation exception
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[Unit] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield ()

  def validate(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[Unit] =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: SecretKey, options: JwtOptions): Try[Unit] =
    validate(token, key, JwtAlgorithm.allHmac(), options)

  def validate(token: String, key: SecretKey): Try[Unit] = validate(token, key, JwtOptions.DEFAULT)

  /** An alias of `validate` in case you want to directly pass a string key.
    *
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    * @throws JwtValidationException default validation exception
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    * @throws IllegalArgumentException couldn't decode the token since it's not a valid base64 string
    */
  def validate(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[Unit] = for {
    (header64, header, claim64, claim, signature) <- splitToken(token)
    h <- parseHeader(header)
    c <- parseClaim(claim)
    _ <- validate(header64, h, claim64, c, signature, key, algorithms, options)
  } yield ()

  def validate(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[Unit] =
    validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: PublicKey, options: JwtOptions): Try[Unit] =
    validate(token, key, JwtAlgorithm.allAsymmetric(), options)

  def validate(token: String, key: PublicKey): Try[Unit] = validate(token, key, JwtOptions.DEFAULT)

  /** Test if a token is valid. Doesn't throw any exception.
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    */
  def isValid(token: String, options: JwtOptions): Boolean = validate(token, options).isSuccess

  def isValid(token: String): Boolean = isValid(token, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for HMAC algorithms
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Boolean = validate(token, key, algorithms, options).isSuccess

  def isValid(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for asymmetric algorithms
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Boolean = validate(token, key, algorithms, options).isSuccess

  def isValid(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Boolean = validate(token, key, algorithms, options).isSuccess

  def isValid(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: SecretKey, options: JwtOptions): Boolean =
    isValid(token, key, JwtAlgorithm.allHmac(), options)

  def isValid(token: String, key: SecretKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Boolean = validate(token, key, algorithms, options).isSuccess

  def isValid(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Boolean =
    isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: PublicKey, options: JwtOptions): Boolean =
    isValid(token, key, JwtAlgorithm.allAsymmetric(), options)

  def isValid(token: String, key: PublicKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)
}
