package pdi.jwt

import java.security.{Key, PrivateKey, PublicKey}
import javax.crypto.SecretKey
import scala.util.Try

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.{
  JwtEmptyAlgorithmException,
  JwtNonEmptyAlgorithmException,
  JwtValidationException
}

trait JwtJsonCommon[J, H, C] extends JwtCore[H, C] {
  protected def parse(value: String): J
  protected def stringify(value: J): String
  protected def getAlgorithm(header: J): Option[JwtAlgorithm]

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

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
}
