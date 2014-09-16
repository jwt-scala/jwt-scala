package pdi.scala.jwt

import scala.util.Try

object Jwt extends JwtCore[String, String] {
  def decodeAll(token: String, maybeKey: Option[String] = None): Try[(String, String, Option[String])] =
    decodeRawAllValidated(token, maybeKey)
}

trait JwtCore[H, C] {
  // Encode
  def encode(header: String, claim: String, key: Option[String] = None, algorithm: Option[String] = None): String = {
    val header64 = JwtBase64.encodeString(header)
    val claim64 = JwtBase64.encodeString(claim)
    Seq(
      header64,
      claim64,
      JwtBase64.encodeString(JwtUtils.sign(header64 + "." + claim64, key, algorithm))
    ).mkString(".")
  }

  def encode(header: String, claim: String, key: String, algorithm: String): String =
    encode(header, claim, Option(key), Option(algorithm))

  def encode(header: JwtHeader, claim: JwtClaim): String =
    encode(header.toJson, claim.toJson, None, None)

  def encode(header: JwtHeader, claim: JwtClaim, key: String): String =
    encode(header.toJson, claim.toJson, Option(key), header.algorithm)

  // Decode
  def decodeRawAll(token: String): Try[(String, String, Option[String])] = Try {
    val parts = token.split("\\.")

    parts.length match {
      case 2 => (JwtBase64.decodeString(parts(0)), JwtBase64.decodeString(parts(1)), None)
      case 3 => (JwtBase64.decodeString(parts(0)), JwtBase64.decodeString(parts(1)), Option(parts(2)))
      case _ => throw new JwtLengthException(s"Expected token [$token] to be composed of 2 or 3 parts separated by dots.")
    }
  }

  def decodeRaw(token: String): Try[String] = decodeRawAll(token).map(_._2)

  protected def decodeRawAllValidated(token: String, maybeKey: Option[String] = None): Try[(String, String, Option[String])] =
    Try {
      if (validate(token, maybeKey)) {
        decodeRawAll(token).get
      } else {
        throw JwtValidationException
      }
    }

  def decodeAll(token: String, maybeKey: Option[String] = None): Try[(H, C, Option[String])]

  def decodeAll(token: String, key: String): Try[(H, C, Option[String])] = decodeAll(token, Option(key))

  def decode(token: String, maybeKey: Option[String] = None): Try[C] = decodeAll(token, maybeKey).map(_._2)

  def decode(token: String, key: String): Try[C] = decode(token, Option(key))

  // Validate
  private val extractAlgorithmRegex = "\"alg\":\"([a-zA-Z0-9]+)\"".r
  private def extractAlgorithm(header64: String): Option[String] = for {
    extractAlgorithmRegex(algo) <- extractAlgorithmRegex findFirstIn JwtBase64.decodeString(header64)
  } yield algo

  def validate(token: String, maybeKey: Option[String] = None): Boolean = {
    val parts = token.split("\\.")
    val maybeAlgo = if (parts.length > 0) { extractAlgorithm(parts(0)) } else { None }

    parts.length match {
      // No signature => no algo in header and no key
      case 2 => maybeKey.isEmpty && maybeAlgo.isEmpty
      // Same as 2
      case 3 if parts(2).isEmpty => maybeKey.isEmpty && maybeAlgo.isEmpty
      // Signature => need to match
      case 3 => java.util.Arrays.equals(JwtBase64.decode(parts(2)), JwtUtils.sign(parts(0) +"."+ parts(1), maybeKey, maybeAlgo))
      // WTF?
      case _ => false
    }
  }

  def validate(token: String, key: String): Boolean = validate(token, Option(key))
}
