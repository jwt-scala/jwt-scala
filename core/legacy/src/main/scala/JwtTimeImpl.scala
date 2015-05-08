package pdi.jwt

import java.util.{Calendar, TimeZone}

trait JwtTimeImpl {
  val TimeZoneUTC = TimeZone.getTimeZone("UTC")

  /** Returns the number of millis since the 01.01.1970
    *
    * @return Returns the number of millis since the 01.01.1970
    */
  def now
  def now: Long = Calendar.getInstance(TimeZoneUTC).getTimeInMillis

  def format(time: Long): String = {
    val cal = Calendar.getInstance(TimeZoneUTC)
    cal.setTimeInMillis(time)
    cal.getTime.toString
  }
}
