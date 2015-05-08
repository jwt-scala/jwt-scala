package pdi.jwt

import java.time.Instant

trait JwtTimeImpl {
  /** Returns the number of millis since the 01.01.1970
    *
    * @return Returns the number of millis since the 01.01.1970
    */
  def now: Long = Instant.now().toEpochMilli

  def format(time: Long): String = Instant.ofEpochMilli(time).toString
}
