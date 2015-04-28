package pdi.jwt

import mockit.MockUp
import mockit.Mock
import java.time.Instant

trait TimeFixtureImpl {
  def mockTime(now: Long) = {
    new MockUp[Instant]() {
      @Mock
      def toEpochMilli: Long = now
    }
  }
}
