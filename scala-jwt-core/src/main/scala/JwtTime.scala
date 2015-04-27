package pdi.jwt

object JwtTime extends JwtTimeImpl {
  def nowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    val timeNow = now
    start.map(_ <= timeNow).getOrElse(true) && end.map(_ >= timeNow).getOrElse(true)
  }

  def validateNowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    val timeNow = now
    start.map(notBefore => if (timeNow < notBefore) {
      throw new JwtNotBeforeException("The token will only be valid after " + format(notBefore) + " - " + notBefore, notBefore)
    } else {
      true
    }).getOrElse(true) && end.map(expiration => if (timeNow > expiration) {
      throw new JwtExpirationException("The token is expired since " + format(expiration) + " - " + expiration, expiration)
    } else {
      true
    }).getOrElse(true)
  }
}
