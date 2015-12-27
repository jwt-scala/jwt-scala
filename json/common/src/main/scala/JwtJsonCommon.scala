package pdi.jwt

import scala.util.Try
import javax.crypto.SecretKey
import java.security.{Key, PrivateKey, PublicKey}

import pdi.jwt.algorithms._
import pdi.jwt.exceptions.{JwtNonEmptyAlgorithmException, JwtEmptyAlgorithmException, JwtValidationException}

trait JwtJsonCommon[J] extends JwtCore[JwtHeader, JwtClaim] {
  protected def parse(value: String): J
  protected def stringify(value: J): String
  protected def getAlgorithm(header: J): Option[JwtAlgorithm]

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

  def encode(header: J, claim: J): String = getAlgorithm(header) match {
    case None => encode(stringify(header), stringify(claim))
    case _ => throw new JwtNonEmptyAlgorithmException()
  }

  def encode(header: J, claim: J, key: String): String = getAlgorithm(header) match {
    case Some(algo: JwtAlgorithm) => encode(stringify(header), stringify(claim), key, algo)
    case _ => throw new JwtEmptyAlgorithmException()
  }

  def encode(header: J, claim: J, key: Key): String = (getAlgorithm(header), key) match {
    case (Some(algo: JwtHmacAlgorithm), k: SecretKey) => encode(stringify(header), stringify(claim), k, algo)
    case (Some(algo: JwtAsymetricAlgorithm), k: PrivateKey) => encode(stringify(header), stringify(claim), k, algo)
    case _ => throw new JwtValidationException("The key type doesn't match the algorithm type. It's either a SecretKey and a HMAC algorithm or a PrivateKey and a RSA or ECDSA algorithm. And an algorithm is required of course.")
  }

  def encode(claim: J): String =
    encode(stringify(claim))

  def encode(claim: J, key: String, algorithm: JwtAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def encode(claim: J, key: SecretKey, algorithm: JwtHmacAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def encode(claim: J, key: PrivateKey, algorithm: JwtAsymetricAlgorithm): String =
    encode(stringify(claim), key, algorithm)

  def decodeJsonAll(token: String, options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String): Try[(J, J, String)] =
    decodeJsonAll(token, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: String, algorithms: => Seq[JwtAsymetricAlgorithm], options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: String, algorithms: => Seq[JwtAsymetricAlgorithm]): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: SecretKey, options: JwtOptions): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtAlgorithm.allHmac, options)

  def decodeJsonAll(token: String, key: SecretKey): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymetricAlgorithm], options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, key, algorithms, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: PublicKey, algorithms: Seq[JwtAsymetricAlgorithm]): Try[(J, J, String)] =
    decodeJsonAll(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJsonAll(token: String, key: PublicKey, options: JwtOptions): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtAlgorithm.allAsymetric, options)

  def decodeJsonAll(token: String, key: PublicKey): Try[(J, J, String)] =
    decodeJsonAll(token, key, JwtOptions.DEFAULT)

  def decodeJson(token: String, options: JwtOptions): Try[J] =
    decodeJsonAll(token, options).map(_._2)

  def decodeJson(token: String): Try[J] =
    decodeJson(token, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: String, algorithms: Seq[JwtHmacAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: String, algorithms: => Seq[JwtAsymetricAlgorithm], options: JwtOptions): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: String, algorithms: => Seq[JwtAsymetricAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm], options: JwtOptions): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: SecretKey, algorithms: Seq[JwtHmacAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: SecretKey, options: JwtOptions): Try[J] =
    decodeJson(token, key, JwtAlgorithm.allHmac, options)

  def decodeJson(token: String, key: SecretKey): Try[J] =
    decodeJson(token, key, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: PublicKey, algorithms: Seq[JwtAsymetricAlgorithm], options: JwtOptions): Try[J] =
    decodeJsonAll(token, key, algorithms, options).map(_._2)

  def decodeJson(token: String, key: PublicKey, algorithms: Seq[JwtAsymetricAlgorithm]): Try[J] =
    decodeJson(token, key, algorithms, JwtOptions.DEFAULT)

  def decodeJson(token: String, key: PublicKey, options: JwtOptions): Try[J] =
    decodeJson(token, key, JwtAlgorithm.allAsymetric, options)

  def decodeJson(token: String, key: PublicKey): Try[J] =
    decodeJson(token, key, JwtOptions.DEFAULT)
}
