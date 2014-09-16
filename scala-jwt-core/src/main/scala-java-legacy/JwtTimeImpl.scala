package pdi.scala.jwt

java.util.Calendar

trait JwtTimeImpl {
  val TimeZoneUTC = TimeZone.getTimeZone("UTC")

  def now: Long = Calendar.getInstance(TimeZoneUTC).getTimeInMillis
}
