package pdi.jwt

import scala.util.Try

trait JwtJsonCommon[J] extends JwtCore[JwtHeader, JwtClaim] {
  protected def parse(value: String): J
  protected def stringify(value: J): String
  protected def getAlgorithm(header: J): Option[JwtAlgorithm]

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

  def encode(header: J, claim: J, key: Option[String]): String =
    encode(stringify(header), stringify(claim), key, getAlgorithm(header))

  def encode(header: J, claim: J, key: String): String =
    encode(header, claim, Option(key))

  def encode(header: J, claim: J): String =
    encode(header, claim, None)

  def encode(claim: J, key: Option[String], algorithm: Option[JwtAlgorithm]): String =
    encode(parse(JwtHeader(algorithm).toJson), claim, key)

  def encode(claim: J, key: String, algorithm: JwtAlgorithm): String =
    encode(claim, Option(key), Option(algorithm))

  def encode(claim: J): String =
    encode(claim, None, None)

  def decodeJsonAll(token: String, maybeKey: Option[String] = None): Try[(J, J, Option[String])] =
    decodeRawAll(token, maybeKey).map { tuple => (parse(tuple._1), parse(tuple._2), tuple._3) }

  def decodeJsonAll(token: String, key: String): Try[(J, J, Option[String])] =
    decodeJsonAll(token, Option(key))

  def decodeJson(token: String, maybeKey: Option[String] = None): Try[J] =
    decodeJsonAll(token, maybeKey).map(_._2)

  def decodeJson(token: String, key: String): Try[J] =
    decodeJson(token, Option(key))
}
