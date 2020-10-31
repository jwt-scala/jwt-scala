package pdi.jwt.exceptions

import pdi.jwt.JwtTime

sealed abstract class JwtException(message: String) extends RuntimeException(message)

class JwtLengthException(message: String) extends JwtException(message)

class JwtValidationException(message: String) extends JwtException(message)

class JwtSignatureFormatException(message: String) extends JwtException(message)

class JwtEmptySignatureException() extends JwtException("No signature found inside the token while trying to verify it with a key.")

class JwtNonEmptySignatureException() extends JwtException("Non-empty signature found inside the token while trying to verify without a key.")

class JwtEmptyAlgorithmException() extends JwtException("No algorithm found inside the token header while having a key to sign or verify it.")

class JwtNonEmptyAlgorithmException() extends JwtException("Algorithm found inside the token header while trying to sign or verify without a key.")

class JwtExpirationException(expiration: Long) extends JwtException("The token is expired since " + JwtTime.format(expiration))

class JwtNotBeforeException(notBefore: Long) extends JwtException("The token will only be valid after " + JwtTime.format(notBefore))

class JwtNonSupportedAlgorithm(algo: String) extends JwtException(s"The algorithm [$algo] is not currently supported.")

class JwtNonSupportedCurve(curve: String) extends JwtException(s"The curve [$curve] is not currently supported.")

class JwtNonStringException(key: String) extends JwtException(s"During JSON parsing, expected a String for key [$key]") {
  def getKey = key
}

class JwtNonStringSetOrStringException(key: String) extends JwtException(s"During JSON parsing, expected a Set[String] or String for key [$key]") {
  def getKey = key
}

class JwtNonNumberException(key: String) extends JwtException(s"During JSON parsing, expected a Number for key [$key]") {
  def getKey = key
}
