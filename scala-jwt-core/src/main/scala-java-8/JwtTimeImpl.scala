package pdi.jwt

import java.time.Instant

trait JwtTimeImpl {
  def now: Long = Instant.now().toEpochMilli

  protected def format(time: Long): String = Instant.ofEpochMilli(time).toString
}
