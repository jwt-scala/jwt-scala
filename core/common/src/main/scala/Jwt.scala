package pdi.jwt

import scala.util.Try
import javax.crypto.SecretKey
import java.security.{Key, PrivateKey, PublicKey}

import pdi.jwt.algorithms._
import pdi.jwt.exceptions._

/**
  * Default implementation of [[JwtCore]] using only Strings. Most of the time, you should use a lib
  * implementing JSON and shouldn't be using this object. But just in case you need pure Scala support,
  * here it is.
  *
  * To see a full list of samples, check the [[http://pauldijou.fr/jwt-scala/samples/jwt-core/ online documentation]].
  *
  * '''Warning''': since there is no JSON support in Scala, this object doesn't have any way to parse
  * a JSON string as an AST, so it only uses regex with all the limitations it implies. Try not to use
  * keys like `exp` and `nbf` in sub-objects of the claim. For example, if you try to use the following
  * claim: `{"user":{"exp":1},"exp":1300819380}`, it should be correct but it will fail because the regex
  * extracting the expiration will return `1` instead of `1300819380`. Sorry about that.
  */
object Jwt extends JwtCore[String, String] {
  protected def parseHeader(header: String): String = header
  protected def parseClaim(claim: String): String = claim

  private val extractAlgorithmRegex = "\"alg\" *: *\"([a-zA-Z0-9]+)\"".r
  protected def extractAlgorithm(header: String): Option[JwtAlgorithm] =
    (extractAlgorithmRegex findFirstMatchIn header).map(_.group(1)).flatMap {
      case "none" => None
      case name: String => Some(JwtAlgorithm.fromString(name))
    }

  private val extractExpirationRegex = "\"exp\" *: *([0-9]+)".r
  protected def extractExpiration(claim: String): Option[Long] =
    (extractExpirationRegex findFirstMatchIn claim).map(_.group(1)).map(_.toLong)

  private val extractNotBeforeRegex = "\"nbf\" *: *([0-9]+)".r
  protected def extractNotBefore(claim: String): Option[Long] =
    (extractNotBeforeRegex findFirstMatchIn claim).map(_.group(1)).map(_.toLong)
}

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
  * @define algos a list of possible algorithms that the token can use. See [[http://pauldijou.fr/jwt-scala/#security-concerns Security concerns]] for more infos.
  *
  */
trait JwtCore[H, C] {
  // Abstract methods
  protected def parseHeader(header: String): H
  protected def parseClaim(claim: String): C

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

  def encode(header: String, claim: String, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String = {
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
    case _ => throw new JwtNonEmptyAlgorithmException()
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
    case _ => throw new JwtEmptyAlgorithmException()
  }

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be deduced from the header.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    * @param key the secret key to use to sign the token (note that the algorithm will be deduced from the header)
    */
  def encode(header: JwtHeader, claim: JwtClaim, key: Key): String = (header.algorithm, key) match {
    case (Some(algo: JwtHmacAlgorithm), k: SecretKey) => encode(header.toJson, claim.toJson, k, algo)
    case (Some(algo: JwtAsymmetricAlgorithm), k: PrivateKey) => encode(header.toJson, claim.toJson, k, algo)
    case _ => throw new JwtValidationException("The key type doesn't match the algorithm type. It's either a SecretKey and a HMAC algorithm or a PrivateKey and a RSA or ECDSA algorithm. And an algorithm is required of course.")
  }

  /**
    * @return a tuple of (header64, header, claim64, claim, signature or empty string if none)
    * @throws JwtLengthException if there is not 2 or 3 parts in the token
    */
  private def splitToken(token: String): (String, String, String, String, String) = {
    val parts = token.split("\\.")

    val signature = parts.length match {
      case 2 => ""
      case 3 => parts(2)
      case _ => throw new JwtLengthException(s"Expected token [$token] to be composed of 2 or 3 parts separated by dots.")
    }

    (parts(0), JwtBase64.decodeString(parts(0)), parts(1), JwtBase64.decodeString(parts(1)), signature)
  }

  /** Will try to decode a JSON Web Token to raw strings
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    */
  def decodeRawAll(token: String, options: JwtOptions): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(parseHeader(header), parseClaim(claim), signature, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String): Try[(String, String, String)] = decodeRawAll(token, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using an asymmetric algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using a HMAC algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeRawAll(token: String, key: SecretKey, options: JwtOptions): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtAlgorithm.allHmac, options)

  def decodeRawAll(token: String, key: SecretKey): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtOptions.DEFAULT)

  /** Will try to decode a JSON Web Token to raw strings using an asymmetric algorithm
    *
    * @return if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRawAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[(String, String, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[(String, String, String)] =
    decodeRawAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeRawAll(token: String, key: PublicKey, options: JwtOptions): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtAlgorithm.allAsymmetric, options)

  def decodeRawAll(token: String, key: PublicKey): Try[(String, String, String)] =
    decodeRawAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    */
  def decodeRaw(token: String, options: JwtOptions): Try[String] = decodeRawAll(token, options).map(_._2)

  def decodeRaw(token: String): Try[String] = decodeRaw(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRaw(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[String] =
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
  def decodeRaw(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRaw(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    */
  def decodeRaw(token: String, key: SecretKey, options: JwtOptions): Try[String] = decodeRaw(token, key, JwtAlgorithm.allHmac, options)

  def decodeRaw(token: String, key: SecretKey): Try[String] = decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeRaw(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[String] =
    decodeRawAll(token, key, algorithms, options).map(_._2)

  def decodeRaw(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[String] =
    decodeRaw(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param key $key
    */
  def decodeRaw(token: String, key: PublicKey, options: JwtOptions): Try[String] = decodeRaw(token, key, JwtAlgorithm.allAsymmetric, options)

  def decodeRaw(token: String, key: PublicKey): Try[String] = decodeRaw(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    */
  def decodeAll(token: String, options: JwtOptions): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(h, c, signature, options)
    (h, c, signature)
  }

  def decodeAll(token: String): Try[(H, C, String)] = decodeAll(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    */
  def decodeAll(token: String, key: SecretKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allHmac, options)

  def decodeAll(token: String, key: SecretKey): Try[(H, C, String)] =
    decodeAll(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decodeAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[(H, C, String)] = Try {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(header64, h, claim64, c, signature, key, algorithms, options)
    (h, c, signature)
  }

  def decodeAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[(H, C, String)] =
    decodeAll(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    */
  def decodeAll(token: String, key: PublicKey, options: JwtOptions): Try[(H, C, String)] =
    decodeAll(token, key, JwtAlgorithm.allAsymmetric, options)

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
  def decode(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[C] =
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
  def decode(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[C] =
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
  def decode(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    */
  def decode(token: String, key: SecretKey, options: JwtOptions): Try[C] = decode(token, key, JwtAlgorithm.allHmac, options)

  def decode(token: String, key: SecretKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def decode(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Try[C] =
    decodeAll(token, key, algorithms, options).map(_._2)

  def decode(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[C] =
    decode(token, key, algorithms, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    */
  def decode(token: String, key: PublicKey, options: JwtOptions): Try[C] = decode(token, key, JwtAlgorithm.allAsymmetric, options)

  def decode(token: String, key: PublicKey): Try[C] = decode(token, key, JwtOptions.DEFAULT)

  // Validate
  protected def extractAlgorithm(header: H): Option[JwtAlgorithm]
  protected def extractExpiration(claim: C): Option[Long]
  protected def extractNotBefore(claim: C): Option[Long]

  protected def validateTiming(claim: C, options: JwtOptions) {
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
  protected def validateHmacAlgorithm(algorithm: JwtHmacAlgorithm, algorithms: Seq[JwtHmacAlgorithm]): Boolean = {
    algorithms.contains(algorithm)
  }

  // Validate if an algorithm is inside the authorized range
  protected def validateAsymmetricAlgorithm(algorithm: JwtAsymmetricAlgorithm, algorithms: Seq[JwtAsymmetricAlgorithm]): Boolean = {
    algorithms.contains(algorithm)
  }

  // Validation when no key and no algorithm
  protected def validate(header: H, claim: C, signature: String, options: JwtOptions) {
    if (options.signature && !signature.isEmpty) {
      throw new JwtNonEmptySignatureException()
    } else if (options.signature && !extractAlgorithm(header).isEmpty) {
      throw new JwtNonEmptyAlgorithmException()
    }

    validateTiming(claim, options)
  }

  // Validation when both key and algorithm
  protected def validate(
    header64: String,
    header: H,
    claim64: String,
    claim: C,
    signature: String,
    options: JwtOptions,
    verify: (Array[Byte], Array[Byte], JwtAlgorithm) => Boolean): Unit = {

    if (options.signature) {
      val maybeAlgo = extractAlgorithm(header)

      if (options.signature && signature.isEmpty) {
        throw new JwtEmptySignatureException()
      } else if (maybeAlgo.isEmpty) {
        throw new JwtEmptyAlgorithmException()
      } else if (!verify(JwtUtils.bytify(header64 +"."+ claim64), JwtBase64.decode(signature), maybeAlgo.get)) {
        throw new JwtValidationException("Invalid signature for this token or wrong algorithm.")
      }
    }
    validateTiming(claim, options)
  }

  // Generic validation on String Key for HMAC algorithms
  protected def validate(header64: String, header: H, claim64: String, claim: C, signature: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Unit = {
    validate(header64, header, claim64, claim, signature, options, (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) => algorithm match {
      case algo: JwtHmacAlgorithm => validateHmacAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
      case _ => false
    })
  }

  // Generic validation on String Key for asymmetric algorithms
  protected def validate(header64: String, header: H, claim64: String, claim: C, signature: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Unit = {
    validate(header64, header, claim64, claim, signature, options, (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) => algorithm match {
      case algo: JwtAsymmetricAlgorithm => validateAsymmetricAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
      case _ => false
    })
  }

  // Validation for HMAC algorithm using a SecretKey
  protected def validate(header64: String, header: H, claim64: String, claim: C, signature: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Unit = {
    validate(header64, header, claim64, claim, signature, options, (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) => algorithm match {
      case algo: JwtHmacAlgorithm => validateHmacAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
      case _ => false
    })
  }

  // Validation for RSA and ECDSA algorithms using PublicKey
  protected def validate(header64: String, header: H, claim64: String, claim: C, signature: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Unit = {
    validate(header64, header, claim64, claim, signature, options, (data: Array[Byte], signature: Array[Byte], algorithm: JwtAlgorithm) => algorithm match {
      case algo: JwtAsymmetricAlgorithm => validateAsymmetricAlgorithm(algo, algorithms) && JwtUtils.verify(data, signature, key, algo)
      case _ => false
    })
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
  def validate(token: String, options: JwtOptions): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(parseHeader(header), parseClaim(claim), signature, options)
  }

  def validate(token: String): Unit = validate(token, JwtOptions.DEFAULT)

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
  def validate(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
  }

  def validate(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Unit = validate(token, key, algorithms, JwtOptions.DEFAULT)

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
  def validate(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
  }

  def validate(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Unit = validate(token, key, algorithms, JwtOptions.DEFAULT)

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
  def validate(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
  }

  def validate(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Unit = validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: SecretKey, options: JwtOptions): Unit = validate(token, key, JwtAlgorithm.allHmac, options)

  def validate(token: String, key: SecretKey): Unit = validate(token, key, JwtOptions.DEFAULT)

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
  def validate(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Unit = {
    val (header64, header, claim64, claim, signature) = splitToken(token)
    validate(header64, parseHeader(header), claim64, parseClaim(claim), signature, key, algorithms, options)
  }

  def validate(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Unit = validate(token, key, algorithms, JwtOptions.DEFAULT)

  def validate(token: String, key: PublicKey, options: JwtOptions): Unit = validate(token, key, JwtAlgorithm.allAsymmetric, options)

  def validate(token: String, key: PublicKey): Unit = validate(token, key, JwtOptions.DEFAULT)

  /** Test if a token is valid. Doesn't throw any exception.
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    */
  def isValid(token: String, options: JwtOptions): Boolean =
    try {
      validate(token, options)
      true
    } catch {
      case _ : Throwable => false
    }

  def isValid(token: String): Boolean = isValid(token, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for HMAC algorithms
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Boolean =
    try {
      validate(token, key, algorithms, options)
      true
    } catch {
      case _ : Throwable => false
    }

  def isValid(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Boolean = isValid(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key for asymmetric algorithms
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Boolean =
    try {
      validate(token, key, algorithms, options)
      true
    } catch {
      case _ : Throwable => false
    }

  def isValid(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Boolean = isValid(token, key, algorithms, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Boolean =
    try {
      validate(token, key, algorithms, options)
      true
    } catch {
      case _ : Throwable => false
    }

  def isValid(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Boolean = isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: SecretKey, options: JwtOptions): Boolean = isValid(token, key, JwtAlgorithm.allHmac, options)

  def isValid(token: String, key: SecretKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    * @param algorithms $algos
    */
  def isValid(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm], options: JwtOptions): Boolean =
    try {
      validate(token, key, algorithms, options)
      true
    } catch {
      case _ : Throwable => false
    }

  def isValid(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Boolean = isValid(token, key, algorithms, JwtOptions.DEFAULT)

  def isValid(token: String, key: PublicKey, options: JwtOptions): Boolean = isValid(token, key, JwtAlgorithm.allAsymmetric, options)

  def isValid(token: String, key: PublicKey): Boolean = isValid(token, key, JwtOptions.DEFAULT)
}
