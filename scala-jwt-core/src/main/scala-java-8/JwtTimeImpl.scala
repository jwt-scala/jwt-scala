package pdi.scala.jwt

import java.time.Instant

trait JwtTimeImpl {
  def now: Long = Instant.now().toEpochMilli

  def nowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    val timeNow = now
    start.map(_ <= timeNow).getOrElse(true) && end.map(_ >= timeNow).getOrElse(true)
  }

  def validateNowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    val timeNow = now
    start.map(notBefore => if (timeNow < notBefore) {
      throw new JwtNotBeforeException("", notBefore)
    } else {
      true
    }).getOrElse(true) && end.map(expiration => if (timeNow > expiration) {
      throw new JwtExpirationException("", expiration)
    } else {
      true
    }).getOrElse(true)
  }
}
