package pdi.jwt

import mockit.MockUp
import mockit.Mock
import java.util.Calendar

trait TimeFixtureImpl {
  def mockTime(now: Long) = {
    new MockUp[Calendar]() {
      @Mock
      def getTimeInMillis: Long = now
    }
  }
}
