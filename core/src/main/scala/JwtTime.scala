package pdi.jwt

import java.time.{Clock, Instant}
import pdi.jwt.exceptions.{JwtNotBeforeException, JwtExpirationException}
import scala.util.{Failure, Success, Try}

/** Util object to handle time operations */
object JwtTime {

  /** Returns the number of millis since the 01.01.1970
    *
    * @return Returns the number of millis since the 01.01.1970
    */
  def now(implicit clock: Clock): Long = clock.instant().toEpochMilli

  /** Returns the number of seconds since the 01.01.1970
    *
    * @return Returns the number of seconds since the 01.01.1970
    */
  def nowSeconds(implicit clock: Clock): Long = this.now / 1000

  def format(time: Long): String = Instant.ofEpochMilli(time).toString

  /** Test if the current time is between the two prams
    *
    * @return the result of the test
    * @param start if set, the instant that must be before now (in millis)
    * @param end if set, the instant that must be after now (in millis)
    */
  def nowIsBetween(start: Option[Long], end: Option[Long])(implicit clock: Clock): Boolean =
    validateNowIsBetween(start, end).isSuccess

  /** Same as `nowIsBetween` but using seconds rather than millis.
    *
    * @param start if set, the instant that must be before now (in seconds)
    * @param end if set, the instant that must be after now (in seconds)
    */
  def nowIsBetweenSeconds(start: Option[Long], end: Option[Long])(implicit clock: Clock): Boolean =
    nowIsBetween(start.map(_ * 1000), end.map(_ * 1000))

  /** Test if the current time is between the two params and throw an exception if we don't have `start` <= now < `end`
    *
    * @param start if set, the instant that must be before now (in millis)
    * @param end if set, the instant that must be after now (in millis)
    * @return Failure(JwtNotBeforeException) if `start` > now
    * @return Failure(JwtExpirationException) if now >= `end`
    */
  def validateNowIsBetween(start: Option[Long], end: Option[Long])(implicit
      clock: Clock
  ): Try[Unit] = {
    val timeNow = now

    (start, end) match {
      case (Some(s), _) if s > timeNow  => Failure(new JwtNotBeforeException(s))
      case (_, Some(e)) if e <= timeNow => Failure(new JwtExpirationException(e))
      case _                            => Success(())
    }
  }

  /** Same as `validateNowIsBetween` but using seconds rather than millis.
    *
    * @param start if set, the instant that must be before now (in seconds)
    * @param end if set, the instant that must be after now (in seconds)
    * @throws JwtNotBeforeException if `start` > now
    * @throws JwtExpirationException if now > `end`
    */
  def validateNowIsBetweenSeconds(start: Option[Long], end: Option[Long])(implicit
      clock: Clock
  ): Try[Unit] =
    validateNowIsBetween(start.map(_ * 1000), end.map(_ * 1000))
}
