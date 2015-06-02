package pdi.jwt

import scala.util.Try
import javax.crypto.SecretKey
import java.security.{Key, PrivateKey, PublicKey}

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

  def decodeJsonAll(token: String): Try[(J, J, String)] =
    decodeRawAll(token).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: String): Try[(J, J, String)] =
    decodeRawAll(token, key).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: SecretKey): Try[(J, J, String)] =
    decodeRawAll(token, key).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: PublicKey): Try[(J, J, String)] =
    decodeRawAll(token, key).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJson(token: String): Try[J] =
    decodeJsonAll(token).map(_._2)

  def decodeJson(token: String, key: String): Try[J] =
    decodeJsonAll(token, key).map(_._2)

  def decodeJson(token: String, key: SecretKey): Try[J] =
    decodeJsonAll(token, key).map(_._2)

  def decodeJson(token: String, key: PublicKey): Try[J] =
    decodeJsonAll(token, key).map(_._2)
}
