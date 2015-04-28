package pdi.jwt

import java.util.{Calendar, TimeZone}

trait JwtTimeImpl {
  val TimeZoneUTC = TimeZone.getTimeZone("UTC")

  def now: Long = Calendar.getInstance(TimeZoneUTC).getTimeInMillis

  def format(time: Long): String = Calendar.getInstance(TimeZoneUTC).setTimeInMillis(time).toString
}
