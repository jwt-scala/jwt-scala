package pdi.jwt

import scala.util.Try

import pdi.jwt.exceptions.JwtNonEmptyAlgorithmException

trait JwtJsonCommon[J, H, C] extends JwtCore[H, C] with JwtJsonCommonPlatform[J, H, C] {
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

  def encode(claim: J): String =
    encode(stringify(claim))

  def decodeJsonAll(token: String, options: JwtOptions): Try[(J, J, String)] =
    decodeRawAll(token, options).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String): Try[(J, J, String)] =
    decodeJsonAll(token, JwtOptions.DEFAULT)

  def decodeJson(token: String, options: JwtOptions): Try[J] =
    decodeJsonAll(token, options).map(_._2)

  def decodeJson(token: String): Try[J] =
    decodeJson(token, JwtOptions.DEFAULT)
}
