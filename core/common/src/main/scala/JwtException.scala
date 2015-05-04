package pdi.jwt

sealed trait JwtException

class JwtLengthException(message: String) extends RuntimeException(message) with JwtException

object JwtValidationException extends RuntimeException with JwtException

class JwtExpirationException(expiration: Long) extends RuntimeException("The token is expired since " + JwtTime.format(expiration)) with JwtException

class JwtNotBeforeException(notBefore: Long) extends RuntimeException("The token will only be valid after " + JwtTime.format(notBefore)) with JwtException
