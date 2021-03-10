package pdi.jwt

import java.time.{Clock, Instant, ZoneOffset}

import org.scalatest.matchers.should._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class JwtClaimSpec extends AnyFunSuite with Matchers with ScalaCheckDrivenPropertyChecks {

  val fakeNowSeconds = 1615411490L
  implicit val clock = Clock.fixed(Instant.ofEpochSecond(fakeNowSeconds), ZoneOffset.UTC)
  val claim = JwtClaim()

  test("JwtClaim.+ should add a json") {
    val result = claim + """{"foo": 42}"""
    assert(result.content == """{"foo": 42}""")
  }

  test("JwtClaim.+ should add a key/value") {
    val result = claim + ("foo", 42)
    assert(result.content == """{"foo":42}""")
  }

  test("JwtClaim.++ should add a key/value") {
    val result = claim ++ ("foo" -> 42)
    assert(result.content == """{"foo":42}""")
  }

  test("JwtClaim.expireIn should set the expiration time") {
    forAll { (delta: Long) =>
      val result = claim.expiresIn(delta)
      assert(result.expiration == Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.expireNow should set the expiration time") {
    val result = claim.expiresNow
    assert(result.expiration == Some(fakeNowSeconds))
  }

  test("JwtClaim.expireAt should set the expiration time") {
    forAll { (epoch: Long) =>
      val result = claim.expiresAt(epoch)
      assert(result.expiration == Some(epoch))
    }
  }

  test("JwtClaim.startIn should set the notBefore") {
    forAll { (delta: Long) =>
      val result = claim.startsIn(delta)
      assert(result.notBefore == Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.startAt should set the notBefore") {
    forAll { (epoch: Long) =>
      val result = claim.startsAt(epoch)
      assert(result.notBefore == Some(epoch))
    }
  }

  test("JwtClaim.startNow should set the notBefore") {
    val result = claim.startsNow
    assert(result.notBefore == Some(fakeNowSeconds))
  }

  test("JwtClaim.issuedIn should set the issuedAt") {
    forAll { (delta: Long) =>
      val result = claim.issuedIn(delta)
      assert(result.issuedAt == Some(fakeNowSeconds + delta))
    }
  }

  test("JwtClaim.issuedAt should set the issuedAt") {
    forAll { (epoch: Long) =>
      val result = claim.issuedAt(epoch)
      assert(result.issuedAt == Some(epoch))
    }
  }

  test("JwtClaim.issuedNow should set the issuedAt") {
    val result = claim.issuedNow
    assert(result.issuedAt == Some(fakeNowSeconds))
  }
}
