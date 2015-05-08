package pdi.jwt

sealed trait JwtException

class JwtLengthException(message: String) extends RuntimeException(message) with JwtException

class JwtValidationException(message: String) extends RuntimeException(message) with JwtException

class JwtExpirationException(expiration: Long) extends RuntimeException("The token is expired since " + JwtTime.format(expiration)) with JwtException

class JwtNotBeforeException(notBefore: Long) extends RuntimeException("The token will only be valid after " + JwtTime.format(notBefore)) with JwtException

class JwtNonSupportedAlgorithm(algo: String) extends RuntimeException(s"The algorithm [$algo] is not currently supported.") with JwtException

class JwtNonStringException(key: String) extends RuntimeException(s"During JSON parsing, expected a String for key [$key]") with JwtException {
  def getKey = key
}

class JwtNonNumberException(key: String) extends RuntimeException(s"During JSON parsing, expected a Number for key [$key]") with JwtException {
  def getKey = key
}
