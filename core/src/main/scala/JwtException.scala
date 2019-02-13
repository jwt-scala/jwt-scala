package pdi.jwt.exceptions

import pdi.jwt.JwtTime

sealed trait JwtException

class JwtLengthException(message: String) extends RuntimeException(message) with JwtException

class JwtValidationException(message: String) extends RuntimeException(message) with JwtException

class JwtSignatureFormatException(message: String) extends RuntimeException(message) with JwtException

class JwtEmptySignatureException() extends RuntimeException("No signature found inside the token while trying to verify it with a key.") with JwtException

class JwtNonEmptySignatureException() extends RuntimeException("Non-empty signature found inside the token while trying to verify without a key.") with JwtException

class JwtEmptyAlgorithmException() extends RuntimeException("No algorithm found inside the token header while having a key to sign or verify it.") with JwtException

class JwtNonEmptyAlgorithmException() extends RuntimeException("Algorithm found inside the token header while trying to sign or verify without a key.") with JwtException

class JwtExpirationException(expiration: Long) extends RuntimeException("The token is expired since " + JwtTime.format(expiration)) with JwtException

class JwtNotBeforeException(notBefore: Long) extends RuntimeException("The token will only be valid after " + JwtTime.format(notBefore)) with JwtException

class JwtNonSupportedAlgorithm(algo: String) extends RuntimeException(s"The algorithm [$algo] is not currently supported.") with JwtException

class JwtNonSupportedCurve(curve: String) extends RuntimeException(s"The curve [$curve] is not currently supported.") with JwtException

class JwtNonStringException(key: String) extends RuntimeException(s"During JSON parsing, expected a String for key [$key]") with JwtException {
  def getKey = key
}

class JwtNonStringSetOrStringException(key: String) extends RuntimeException(s"During JSON parsing, expected a Set[String] or String for key [$key]") with JwtException {
  def getKey = key
}

class JwtNonNumberException(key: String) extends RuntimeException(s"During JSON parsing, expected a Number for key [$key]") with JwtException {
  def getKey = key
}
