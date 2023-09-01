package pdi.jwt

import java.time.Clock
import scala.util.Try

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.*

/** Provide the main logic around Base64 encoding / decoding and signature using the correct
  * algorithm. '''H''' and '''C''' types are respesctively the header type and the claim type. For
  * the core project, they will be String but you are free to extend this trait using other types
  * like JsObject or anything else.
  *
  * Please, check implementations, like [[Jwt]], for code samples.
  *
  * @tparam H
  *   the type of the extracted header from a JSON Web Token
  * @tparam C
  *   the type of the extracted claim from a JSON Web Token
  *
  * @define token
  *   a JSON Web Token as a Base64 url-safe encoded String which can be used inside an HTTP header
  * @define headerString
  *   a valid stringified JSON representing the header of the token
  * @define claimString
  *   a valid stringified JSON representing the claim of the token
  * @define key
  *   the key that will be used to check the token signature
  * @define algo
  *   the algorithm to sign the token
  * @define algos
  *   a list of possible algorithms that the token can use. See
  *   [[https://jwt-scala.github.io/jwt-scala/#security-concerns Security concerns]] for more infos.
  */
trait JwtCore[H, C] extends JwtCorePlatform[H, C] {
  implicit private[jwt] def clock: Clock

  // Abstract methods
  protected def parseHeader(header: String): H
  protected def parseClaim(claim: String): C

  protected def extractAlgorithm(header: H): Option[JwtAlgorithm]
  protected def extractExpiration(claim: C): Option[Long]
  protected def extractNotBefore(claim: C): Option[Long]

  def encode(header: String, claim: String): String = {
    JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(claim) + "."
  }

  /** An alias to `encode` which will provide an automatically generated header.
    *
    * @return
    *   $token
    * @param claim
    *   $claimString
    */
  def encode(claim: String): String = encode(JwtHeader().toJson, claim)

  /** An alias to `encode` which will provide an automatically generated header and setting both key
    * and algorithm to None.
    *
    * @return
    *   $token
    * @param claim
    *   the claim of the JSON Web Token
    */
  def encode(claim: JwtClaim): String = encode(claim.toJson)

  /** An alias to `encode` if you want to use case classes for the header and the claim rather than
    * strings, they will just be stringified to JSON format.
    *
    * @return
    *   $token
    * @param header
    *   the header to stringify as a JSON before encoding the token
    * @param claim
    *   the claim to stringify as a JSON before encoding the token
    */
  def encode(header: JwtHeader, claim: JwtClaim): String = header.algorithm match {
    case None => encode(header.toJson, claim.toJson)
    case _    => throw new JwtNonEmptyAlgorithmException()
  }

  /** Will try to decode a JSON Web Token to raw strings
    *
    * @return
    *   if successful, a tuple of 3 strings, the header, the claim and the signature
    * @param token
    *   $token
    */
  def decodeRawAll(token: String, options: JwtOptions): Try[(String, String, String)] = Try {
    val (_, header, _, claim, signature) = splitToken(token)
    validate(parseHeader(header), parseClaim(claim), signature, options)
    (header, claim, signature)
  }

  def decodeRawAll(token: String): Try[(String, String, String)] =
    decodeRawAll(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the
    * time)
    *
    * @return
    *   if successful, a string representing the JSON version of the claim
    * @param token
    *   $token
    */
  def decodeRaw(token: String, options: JwtOptions): Try[String] =
    decodeRawAll(token, options).map(_._2)

  def decodeRaw(token: String): Try[String] = decodeRaw(token, JwtOptions.DEFAULT)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return
    *   if successful, a tuple representing the header, the claim and eventually the signature
    * @param token
    *   $token
    */
  def decodeAll(token: String, options: JwtOptions): Try[(H, C, String)] = Try {
    val (_, header, _, claim, signature) = splitToken(token)
    val (h, c) = (parseHeader(header), parseClaim(claim))
    validate(h, c, signature, options)
    (h, c, signature)
  }

  def decodeAll(token: String): Try[(H, C, String)] = decodeAll(token, JwtOptions.DEFAULT)

  /** Same as `decodeAll` but only return the claim
    *
    * @return
    *   if successful, the claim of the token in its correct type
    * @param token
    *   $token
    */
  def decode(token: String, options: JwtOptions): Try[C] = decodeAll(token, options).map(_._2)

  def decode(token: String): Try[C] = decode(token, JwtOptions.DEFAULT)

  // Validate
  protected def validateTiming(claim: C, options: JwtOptions): Try[Unit] = {
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
  ): Boolean = algorithms.contains(algorithm)

  // Validate if an algorithm is inside the authorized range
  protected def validateAsymmetricAlgorithm(
      algorithm: JwtAsymmetricAlgorithm,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Boolean = algorithms.contains(algorithm)

  // Validation when no key and no algorithm (or unknown)
  protected def validate(header: H, claim: C, signature: String, options: JwtOptions) = {
    if (options.signature) {
      if (!signature.isEmpty) {
        throw new JwtNonEmptySignatureException()
      }

      extractAlgorithm(header).foreach {
        case JwtUnknownAlgorithm(name) => throw new JwtNonSupportedAlgorithm(name)
        case _                         => throw new JwtNonEmptyAlgorithmException()
      }
    }

    validateTiming(claim, options).get
  }

  // Validation when both key and algorithm
  protected def validate(
      header64: String,
      header: H,
      claim64: String,
      claim: C,
      signature: String,
      options: JwtOptions,
      verify: (Array[Byte], Array[Byte], JwtAlgorithm) => Boolean
  ): Unit = {
    if (options.signature) {
      val maybeAlgo = extractAlgorithm(header)

      if (options.signature && signature.isEmpty) {
        throw new JwtEmptySignatureException()
      } else if (maybeAlgo.isEmpty) {
        throw new JwtEmptyAlgorithmException()
      } else if (
        !verify(
          JwtUtils.bytify(header64 + "." + claim64),
          JwtBase64.decode(signature),
          maybeAlgo.get
        )
      ) {
        throw new JwtValidationException("Invalid signature for this token or wrong algorithm.")
      }
    }
    validateTiming(claim, options).get
  }

  /** Valid a token: doesn't return anything but will thrown exceptions if there are any errors.
    *
    * @param token
    *   $token
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
  def validate(token: String, options: JwtOptions): Unit = {
    val (_, header, _, claim, signature) = splitToken(token)
    validate(parseHeader(header), parseClaim(claim), signature, options)
  }

  def validate(token: String): Unit = validate(token, JwtOptions.DEFAULT)

  /** Test if a token is valid. Doesn't throw any exception.
    *
    * @return
    *   a boolean value indicating if the token is valid or not
    * @param token
    *   $token
    */
  def isValid(token: String, options: JwtOptions): Boolean = Try(validate(token, options)).isSuccess

  def isValid(token: String): Boolean = isValid(token, JwtOptions.DEFAULT)
}

trait JwtCoreFunctions {

  /** @return
    *   a tuple of (header64, header, claim64, claim, signature or empty string if none)
    * @throws JwtLengthException
    *   if there is not 2 or 3 parts in the token
    */
  protected def splitToken(token: String): (String, String, String, String, String) = {
    val parts = JwtUtils.splitString(token, '.')

    val signature = parts.length match {
      case 2 => ""
      case 3 => parts(2)
      case _ =>
        throw new JwtLengthException(
          s"Expected token [$token] to be composed of 2 or 3 parts separated by dots."
        )
    }

    (
      parts(0),
      JwtBase64.decodeString(parts(0)),
      parts(1),
      JwtBase64.decodeString(parts(1)),
      signature
    )
  }
}
