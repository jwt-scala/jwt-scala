package pdi.jwt

import java.security.{Key, PrivateKey, PublicKey}
import javax.crypto.SecretKey
import scala.util.Try

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.{
  JwtEmptyAlgorithmException,
  JwtLengthException,
  JwtNonEmptyAlgorithmException,
  JwtUnknownKeyIdException,
  JwtValidationException
}

trait JwtJsonCommon[J, H, C] extends JwtCore[H, C] {
  protected def parse(value: String): J
  protected def stringify(value: J): String
  protected def getAlgorithm(header: J): Option[JwtAlgorithm]

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

  /** The `kid` of a header. Only the headers of type `JwtHeader` carry one, override this for a
    * custom header type to use `decodeWithJwks` with it.
    */
  protected def extractKeyId(header: H): Option[String] = header match {
    case jwtHeader: JwtHeader => jwtHeader.keyId
    case _                    => None
  }

  def encode(header: J, claim: J): String = getAlgorithm(header) match {
    case None => encode(stringify(header), stringify(claim))
    case _    => throw new JwtNonEmptyAlgorithmException()
  }

  def encode(header: J, claim: J, key: String): String = getAlgorithm(header) match {
    case Some(algo: JwtAlgorithm) => encode(stringify(header), stringify(claim), key, algo)
    case _                        => throw new JwtEmptyAlgorithmException()
  }

  def encode(header: J, claim: J, key: Key): String = (getAlgorithm(header), key) match {
    case (Some(algo: JwtHmacAlgorithm), k: SecretKey) =>
      encode(stringify(header), stringify(claim), k, algo)
    case (Some(algo: JwtAsymmetricAlgorithm), k: PrivateKey) =>
      encode(stringify(header), stringify(claim), k, algo)
    case _ =>
      throw new JwtValidationException(
        "The key type doesn't match the algorithm type. It's either a SecretKey and a HMAC algorithm or a PrivateKey and a RSA or ECDSA algorithm. And an algorithm is required of course."
      )
  }

  def encode(claim: J): String =
    encode(stringify(claim))

  def encode(claim: J, key: String, algorithm: JwtAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def encode(claim: J, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def encode(claim: J, key: PrivateKey, algorithm: JwtAsymmetricAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def decodeJsonAll(token: String, options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String): Try[(J, J, String)] =
    decodeJsonAll(token, JwtOptions.DEFAULT)

  def decodeJsonAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple =>
      (parse(tuple._1), parse(tuple._2), tuple._3)
    }

  def decodeJsonAll(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple =>
      (parse(tuple._1), parse(tuple._2), tuple._3)
    }

  def decodeJsonAll(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm]
  ): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple =>
      (parse(tuple._1), parse(tuple._2), tuple._3)
    }

  def decodeJsonAll(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm]
  ): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: SecretKey, options: JwtOptions): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtAlgorithm.allHmac(), options)

  def decodeJsonAll(token: String, key: SecretKey): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtOptions.DEFAULT)

  def decodeJsonAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple =>
      (parse(tuple._1), parse(tuple._2), tuple._3)
    }

  def decodeJsonAll(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: PublicKey, options: JwtOptions): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeJsonAll(token: String, key: PublicKey): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtOptions.DEFAULT)

  def decodeJson(token: String, options: JwtOptions): Try[J] =
    decodeJsonAll(token, options).map(_._2)

  def decodeJson(token: String): Try[J] =
    decodeJson(token, JwtOptions.DEFAULT)

  def decodeJson(
      token: String,
      key: String,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(
      token: String,
      key: String,
      algorithms: => Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: String, algorithms: => Seq[JwtAsymmetricAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(
      token: String,
      key: SecretKey,
      algorithms: Seq[JwtHmacAlgorithm],
      options: JwtOptions
  ): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: SecretKey, options: JwtOptions): Try[J] =
    decodeJson(token, key, JwtAlgorithm.allHmac(), options)

  def decodeJson(token: String, key: SecretKey): Try[J] =
    decodeJson(token, key, JwtOptions.DEFAULT)

  def decodeJson(
      token: String,
      key: PublicKey,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: PublicKey, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: PublicKey, options: JwtOptions): Try[J] =
    decodeJson(token, key, JwtAlgorithm.allAsymmetric(), options)

  def decodeJson(token: String, key: PublicKey): Try[J] =
    decodeJson(token, key, JwtOptions.DEFAULT)

  /** Parse a public JWK. Private members such as `d`, `p` or `q` are ignored. If the document is
    * not valid JSON, the parse error of the JSON library is let through.
    *
    * @throws pdi.jwt.exceptions.JwtValidationException
    *   if the document is not a JSON object, or a key parameter is empty, has the wrong length or
    *   is a point that is not on the curve
    * @throws pdi.jwt.exceptions.JwtNonStringException
    *   if a member is missing or is not a string, or `key_ops` is not an array of strings
    * @throws pdi.jwt.exceptions.JwtNonSupportedAlgorithm
    *   if the key type is not `RSA`, `EC` or `OKP`
    * @throws pdi.jwt.exceptions.JwtNonSupportedCurve
    *   if the curve is not supported
    * @throws java.lang.IllegalArgumentException
    *   if a key parameter is not valid base64url
    */
  def parseJwk(json: String): Jwk

  /** Parse a JWK Set. As per RFC 7517 section 5, keys that are not usable are skipped. If the
    * document is not valid JSON, the parse error of the JSON library is let through.
    *
    * @throws pdi.jwt.exceptions.JwtValidationException
    *   if the document has no `keys` array
    */
  def parseJwkSet(json: String): JwkSet

  /** Decode a token verified with a key of the set that has the `kid` of its header.
    *
    * As kid values need not be unique in a set (RFC 7517 section 4.5), the first key with the `kid`
    * that is usable for the token is selected. If none is, the failure is the one of the first key
    * with the `kid`.
    *
    * The key must agree with the algorithm: `RS*` with an RSA key, `ES256`, `ES384` and `ES512`
    * with an EC key on P-256, P-384 and P-521, `EdDSA` with an OKP key. It must not be meant for
    * encryption, its `key_ops`, when present, must contain `verify`, and its `alg`, when present,
    * must be the one of the header.
    *
    * Fetching and caching the set is left to the caller.
    *
    * @return
    *   if successful, the claim of the token. Otherwise a `Failure` holding a
    *   `JwtUnknownKeyIdException` when the token has a `kid` that no key of the set carries, or
    *   another `JwtException`, e.g. a `JwtValidationException` when the token has no `kid` or the
    *   key found is not usable for the token, or the exception that `decode` gives, e.g. the
    *   `IllegalArgumentException` of a header that is not valid base64url, the parsing failure of a
    *   header that is not valid JSON or the `java.security.SignatureException` of a signature that
    *   the JDK rejects as malformed. These exceptions of `decode` are passed through as they are.
    * @param token
    *   a JSON Web Token as a Base64 url-safe encoded String
    * @param keys
    *   the public keys the token can be signed with
    * @param algorithms
    *   a list of possible algorithms that the token can use. See
    *   [[https://jwt-scala.github.io/jwt-scala/#security-concerns Security concerns]] for more
    *   infos.
    * @param options
    *   the validations to perform on the token once its key is selected
    */
  def decodeWithJwks(
      token: String,
      keys: JwkSet,
      algorithms: Seq[JwtAsymmetricAlgorithm],
      options: JwtOptions
  ): Try[C] =
    Try(selectJwk(token, keys, algorithms))
      .flatMap(jwk => decode(token, jwk.publicKey, algorithms, options))

  def decodeWithJwks(token: String, keys: JwkSet, algorithms: Seq[JwtAsymmetricAlgorithm]): Try[C] =
    decodeWithJwks(token, keys, algorithms, JwtOptions.DEFAULT)

  private def selectJwk(
      token: String,
      keys: JwkSet,
      algorithms: Seq[JwtAsymmetricAlgorithm]
  ): Jwk = {
    val header = tokenHeader(token)
    val algorithm = extractAlgorithm(header) match {
      case None => throw new JwtEmptyAlgorithmException()
      case Some(algo: JwtAsymmetricAlgorithm) if algorithms.contains(algo) => algo
      case Some(_)                                                         =>
        throw new JwtValidationException("Invalid signature for this token or wrong algorithm.")
    }
    val keyId = extractKeyId(header).getOrElse(
      throw new JwtValidationException("The token header has no key id (kid).")
    )
    val candidates = keys.findAll(keyId)
    if (candidates.isEmpty) throw new JwtUnknownKeyIdException(keyId)
    candidates
      .find(unusableReason(_, algorithm).isEmpty)
      .getOrElse(throw new JwtValidationException(unusableReason(candidates.head, algorithm).get))
  }

  private def unusableReason(jwk: Jwk, algorithm: JwtAsymmetricAlgorithm): Option[String] =
    if (!jwk.isCompatibleWith(algorithm))
      Some("The type or curve of the key doesn't match the algorithm.")
    else if (jwk.use.contains(JwkUse.Encryption))
      Some("The key is meant for encryption, not for signatures.")
    else if (jwk.keyOperations.exists(!_.contains("verify")))
      Some("The key is not meant to verify signatures (key_ops).")
    else if (jwk.algorithm.exists(_ != algorithm.name))
      Some("The algorithm of the key differs from the one of the token.")
    else None

  private def tokenHeader(token: String): H = {
    val parts = JwtUtils.splitString(token, '.')
    if (parts.length != 2 && parts.length != 3)
      throw new JwtLengthException(
        "Expected the token to be composed of 2 or 3 parts separated by dots."
      )
    parseHeader(JwtBase64.decodeString(parts(0)))
  }
}
