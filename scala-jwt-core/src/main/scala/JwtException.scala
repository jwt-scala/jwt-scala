package pdi.jwt

sealed trait JwtException

class JwtLengthException(message: String) extends RuntimeException(message) with JwtException

object JwtValidationException extends RuntimeException with JwtException

class JwtExpirationException(message: String, expiration: Long) extends RuntimeException(message) with JwtException

class JwtNotBeforeException(message: String, notBefore: Long) extends RuntimeException(message) with JwtException
