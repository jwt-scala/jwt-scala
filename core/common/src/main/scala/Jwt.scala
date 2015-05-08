package pdi.jwt

import scala.util.Try

/**
  * Default implementation of [[JwtCore]] using only Strings.
  */
object Jwt extends JwtCore[String, String] {
  def decodeAll(token: String, maybeKey: Option[String] = None): Try[(String, String, Option[String])] =
    decodeRawAll(token, maybeKey)
}

/** Provide the main logic around Base64 encoding / decoding and signature using the correct algorithm.
  * '''H''' and '''C''' types are respesctively the header type and the claim type. For the core project,
  * they will be String but you are free to extend this trait using other types like
  * JsObject or anything else. You just need to implement the abstract `decodeAll` function.
  *
  * Please, check implementations, like [[Jwt]], for code samples.
  *
  * @tparam H the type of the extracted header from a JSON Web Token
  * @tparam C the type of the extracted claim from a JSON Web Token
  *
  * @define token a JSON Web Token as a Base64 url-safe encoded String which can be used inside an HTTP header
  * @define headerString a valid stringified JSON representing the header of the token
  * @define claimString a valid stringified JSON representing the claim of the token
  * @define maybeKey an optional key that will be used to check the token signature
  * @define key the key that will be used to check the token signature
  *
  */
trait JwtCore[H, C] {
  /** Encode a JSON Web Token from its different parts. Both the header and the claim will be encoded to Base64 url-safe, then a signature will be eventually generated from it if you did pass a key and an algorithm, and finally, those three parts will be merged as a single string, using dots as separator.
    *
    * @return $token
    * @param header $headerString
    * @param claim $claimString
    * @param key the secret key to use to sign the token. If none, the token will not be signed
    * @param algorithm the algorithm to use to sign the token. If none but there is a key, the default one will be used
    */
  def encode(header: String, claim: String, key: Option[String] = None, algorithm: Option[JwtAlgorithm] = None): String = {
    val header64 = JwtBase64.encodeString(header)
    val claim64 = JwtBase64.encodeString(claim)
    Seq(
      header64,
      claim64,
      JwtBase64.encodeString(JwtUtils.sign(header64 + "." + claim64, key, algorithm))
    ).mkString(".")
  }

  /** An alias to `encode` if you want to directly pass strings for the key and the algorithm
    *
    * @return $token
    * @param header $headerString
    * @param claim $claimString
    * @param key the secret key to use to sign the token
    * @param algorithm the algorithm to use to sign the token
    */
  def encode(header: String, claim: String, key: String, algorithm: JwtAlgorithm): String =
    encode(header, claim, Option(key), Option(algorithm))

  /** An alias to `encode` if you want to use case classes for the header and the claim rather than strings, they will just be stringified to JSON format.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    */
  def encode(header: JwtHeader, claim: JwtClaim): String =
    encode(header.toJson, claim.toJson, None, None)

  /** An alias of `encode` if you only want to pass a string as the key, the algorithm will be deduced from the header.
    *
    * @return $token
    * @param header the header to stringify as a JSON before encoding the token
    * @param claim the claim to stringify as a JSON before encoding the token
    * @param key the secret key to use to sign the token (note that the algorithm will be deduced from the header)
    */
  def encode(header: JwtHeader, claim: JwtClaim, key: String): String =
    encode(header.toJson, claim.toJson, Option(key), header.algorithm)

  /**
    * @return a tuple of (header64, header, claim64, claim, Option(signature as bytes))
    * @throws JwtLengthException if there is not 2 or 3 parts in the token
    */
  private def splitToken(token: String): (String, String, String, String, Option[String]) = {
    val parts = token.split("\\.")

    val maybeSignature = parts.length match {
      case 2 => None
      case 3 => Option(parts(2))
      case _ => throw new JwtLengthException(s"Expected token [$token] to be composed of 2 or 3 parts separated by dots.")
    }

    (parts(0), JwtBase64.decodeString(parts(0)), parts(1), JwtBase64.decodeString(parts(1)), maybeSignature)
  }

  /** Will try to decode a JSON Web Token to raw strings
    *
    * @return if successful, a tuple of 2 strings, the header and the claim, and an optional string for the signature
    * @param token $token
    */
  def decodeRawAll(token: String, maybeKey: Option[String] = None): Try[(String, String, Option[String])] = Try {
    val (header64, header, claim64, claim, maybeSignature) = splitToken(token)
    validate(header64, header, claim64, claim, maybeSignature, maybeKey)
    (header, claim, maybeSignature)
  }

  /** Same as `decodeRawAll` but only return the claim (you only care about the claim most of the time)
    *
    * @return if successful, a string representing the JSON version of the claim
    * @param token $token
    * @param maybeKey $maybeKey
    */
  def decodeRaw(token: String, maybeKey: Option[String] = None): Try[String] = decodeRawAll(token, maybeKey).map(_._2)

  /** Same as `decodeRawAll` but return the real header and claim types
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param maybeKey $maybeKey
    */
  def decodeAll(token: String, maybeKey: Option[String] = None): Try[(H, C, Option[String])]

  /** An alias of `decodeAll` if you want to directly pass a string key rather than an Option
    *
    * @return if successful, a tuple representing the header, the claim and eventually the signature
    * @param token $token
    * @param key $key
    */
  def decodeAll(token: String, key: String): Try[(H, C, Option[String])] = decodeAll(token, Option(key))

  /** Same as `decodeAll` but only return the claim
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param maybeKey $maybeKey
    */
  def decode(token: String, maybeKey: Option[String] = None): Try[C] = decodeAll(token, maybeKey).map(_._2)

  /** An alias of `decode` if you want to directly pass a string key rather than an Option
    *
    * @return if successful, the claim of the token in its correct type
    * @param token $token
    * @param key $key
    */
  def decode(token: String, key: String): Try[C] = decode(token, Option(key))

  // Validate
  private val extractAlgorithmRegex = "\"alg\":\"([a-zA-Z0-9]+)\"".r
  private def extractAlgorithm(header: String): Option[String] = for {
    extractAlgorithmRegex(algo) <- extractAlgorithmRegex findFirstIn header
  } yield algo

  private val extractExpirationRegex = "\"exp\":\"([0-9]+)\"".r
  private def extractExpiration(claim: String): Option[Long] = for {
    extractExpirationRegex(expiration) <- extractExpirationRegex findFirstIn claim
  } yield expiration.toLong

  private val extractNotBeforeRegex = "\"nbf\":\"([0-9]+)\"".r
  private def extractNotBefore(claim: String): Option[Long] = for {
    extractNotBeforeRegex(notBefore) <- extractNotBeforeRegex findFirstIn claim
  } yield notBefore.toLong

  private def validate(
    header64: String,
    header: String,
    claim64: String,
    claim: String,
    maybeSignature: Option[String],
    maybeKey: Option[String]): Unit = {

    // First, let's valid the signature
    val maybeAlgo = extractAlgorithm(header).map(JwtAlgorithm.fromString)

    maybeSignature match {
      case Some(signature) if !java.util.Arrays.equals(JwtBase64.decode(signature), JwtUtils.sign(header64 +"."+ claim64, maybeKey, maybeAlgo)) => {
        throw new JwtValidationException(s"The signature is invalid for this token.")
      }
      // If there is no signature, there must be no key nor algorithm either
      case None if !(maybeKey.isEmpty && maybeAlgo.isEmpty) => {
         throw new JwtValidationException(s"The token didn't have any signature but there was either a secret key or an algorithm, meaning there should be a signature.")
      }
      case _ => Unit
    }

    // Second, let's valid the date (expiration + not before)
    // (remember, those are seconds, not millis)
    val maybeExpiration = extractExpiration(claim)
    val maybeNotBefore = extractNotBefore(claim)

    JwtTime.validateNowIsBetweenSeconds(maybeNotBefore, maybeExpiration)
  }

  /** Valid a token: doesn't return anything but will thrown exceptions if there are any errors.
    *
    * @param token $token
    * @param maybeKey $maybeKey
    * @throws JwtValidationException default validation exeption
    * @throws JwtLengthException the number of parts separated by dots is wrong
    * @throws JwtNotBeforeException the token isn't valid yet because its `notBefore` attribute is in the future
    * @throws JwtExpirationException the token isn't valid anymore because its `expiration` attribute is in the past
    */
  def validate(token: String, maybeKey: Option[String] = None): Unit = {
    val (header64, header, claim64, claim, maybeSignature) = splitToken(token)
    validate(header64, header, claim64, claim, maybeSignature, maybeKey)
  }

  /** An alias of `validate` in case you want to directly pass a string key.
    *
    * @param token $token
    * @param key $key
    */
  def validate(token: String, key: String): Unit = validate(token, Option(key))

  /** Test if a token is valid. Doesn't throw any exception.
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param maybeKey $maybeKey
    */
  def isValid(token: String, maybeKey: Option[String] = None): Boolean =
    try {
      validate(token, maybeKey)
      true
    } catch {
      case _ : Throwable => false
    }

  /** An alias for `isValid` if you want to directly pass a string as the key
    *
    * @return a boolean value indicating if the token is valid or not
    * @param token $token
    * @param key $key
    */
  def isValid(token: String, key: String): Boolean = isValid(token, Option(key))
}
