package pdi.scala.jwt

import java.time.Instant

trait JwtTimeImpl {
  def now: Long = Instant.now().toEpochMilli
}
