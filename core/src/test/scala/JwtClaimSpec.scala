package pdi.jwt

import java.time.{Clock, Instant, ZoneOffset}

import munit.ScalaCheckSuite
import org.scalacheck.Prop._

class JwtClaimSpec extends ScalaCheckSuite {

  val fakeNowSeconds = 1615411490L
  implicit val clock: Clock = Clock.fixed(Instant.ofEpochSecond(fakeNowSeconds), ZoneOffset.UTC)
  val claim = JwtClaim()

  test("JwtClaim.+ should add a json") {
    forAll { (value: Long) =>
      val result = claim + s"""{"foo": $value}"""
      assertEquals(result.content, s"""{"foo": $value}""")
    }
  }

  test("JwtClaim.+ should add a key/value") {
    forAll { (value: Long) =>
      val result = claim + ("foo", value)
      assertEquals(result.content, s"""{"foo":$value}""")
    }
  }

  test("JwtClaim.++ should add a key/value") {
    forAll { (value: Long) =>
      val result = claim ++ ("foo" -> value)
      assertEquals(result.content, s"""{"foo":$value}""")
    }
  }

  test("JwtClaim.expireIn should set the expiration time") {
    forAll { (delta: Long) =>
      val result = claim.expiresIn(delta)
      assertEquals(result.expiration, Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.expireNow should set the expiration time") {
    val result = claim.expiresNow
    assertEquals(result.expiration, Some(fakeNowSeconds))
  }

  test("JwtClaim.expireAt should set the expiration time") {
    forAll { (epoch: Long) =>
      val result = claim.expiresAt(epoch)
      assertEquals(result.expiration, Some(epoch))
    }
  }

  test("JwtClaim.startIn should set the notBefore") {
    forAll { (delta: Long) =>
      val result = claim.startsIn(delta)
      assertEquals(result.notBefore, Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.startAt should set the notBefore") {
    forAll { (epoch: Long) =>
      val result = claim.startsAt(epoch)
      assertEquals(result.notBefore, Some(epoch))
    }
  }

  test("JwtClaim.startNow should set the notBefore") {
    val result = claim.startsNow
    assertEquals(result.notBefore, Some(fakeNowSeconds))
  }

  test("JwtClaim.issuedIn should set the issuedAt") {
    forAll { (delta: Long) =>
      val result = claim.issuedIn(delta)
      assertEquals(result.issuedAt, Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.issuedAt should set the issuedAt") {
    forAll { (epoch: Long) =>
      val result = claim.issuedAt(epoch)
      assertEquals(result.issuedAt, Some(epoch))
    }
  }

  test("JwtClaim.issuedNow should set the issuedAt") {
    val result = claim.issuedNow
    assertEquals(result.issuedAt, Some(fakeNowSeconds))
  }
}
