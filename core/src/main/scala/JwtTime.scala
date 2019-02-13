package pdi.jwt

import java.time.Instant
import pdi.jwt.exceptions.{JwtNotBeforeException, JwtExpirationException}

/** Util object to handle time operations */
object JwtTime {
  /** Returns the number of millis since the 01.01.1970
    *
    * @return Returns the number of millis since the 01.01.1970
    */
  def now: Long = Instant.now().toEpochMilli

  /** Returns the number of seconds since the 01.01.1970
    *
    * @return Returns the number of seconds since the 01.01.1970
    */
  def nowSeconds: Long = this.now / 1000

  def format(time: Long): String = Instant.ofEpochMilli(time).toString

  /** Test if the current time is between the two prams
    *
    * @return the result of the test
    * @param start if set, the instant that must be before now (in millis)
    * @param end if set, the instant that must be after now (in millis)
    */
  def nowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    try {
      validateNowIsBetween(start, end)
      true
    } catch {
      case _ : JwtNotBeforeException => false
      case _ : JwtExpirationException => false
    }
  }

  /** Same as `nowIsBetween` but using seconds rather than millis.
    *
    * @param start if set, the instant that must be before now (in seconds)
    * @param end if set, the instant that must be after now (in seconds)
    */
  def nowIsBetweenSeconds(start: Option[Long], end: Option[Long]): Boolean =
    nowIsBetween(start.map(_ * 1000), end.map(_ * 1000))

  /** Test if the current time is between the two params and throw an exception if we don't have `start` <= now < `end`
    *
    * @param start if set, the instant that must be before now (in millis)
    * @param end if set, the instant that must be after now (in millis)
    * @throws JwtNotBeforeException if `start` > now
    * @throws JwtExpirationException if now >= `end`
    */
  def validateNowIsBetween(start: Option[Long], end: Option[Long]): Unit = {
    val timeNow = now

    if (!start.isEmpty && start.get > timeNow) {
      throw new JwtNotBeforeException(start.get)
    }

    if (!end.isEmpty && timeNow >= end.get) {
      throw new JwtExpirationException(end.get)
    }
  }

  /** Same as `validateNowIsBetween` but using seconds rather than millis.
    *
    * @param start if set, the instant that must be before now (in seconds)
    * @param end if set, the instant that must be after now (in seconds)
    * @throws JwtNotBeforeException if `start` > now
    * @throws JwtExpirationException if now > `end`
    */
  def validateNowIsBetweenSeconds(start: Option[Long], end: Option[Long]): Unit =
    validateNowIsBetween(start.map(_ * 1000), end.map(_ * 1000))
}
