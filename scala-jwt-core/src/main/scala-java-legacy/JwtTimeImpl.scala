package pdi.scala.jwt

java.util.Calendar

trait JwtTimeImpl {
  val TimeZoneUTC = TimeZone.getTimeZone("UTC")

  def now: Long = Calendar.getInstance(TimeZoneUTC).getTimeInMillis

  def nowIsBetween(start: Option[Long], end: Option[Long]): Boolean = {
    val timeNow = now
    start.map(_ <= timeNow).getOrElse(true) && end.map(_ >= timeNow).getOrElse(true)
  }
}
