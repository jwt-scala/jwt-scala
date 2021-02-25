package pdi.jwt

import scala.util._

import org.scalatest.matchers._
import org.scalatest.funspec._
import org.scalatest.matchers.should._

abstract class UnitSpec extends AnyFunSpec with Matchers {
  // HEADER
  def algorithm(expectedValue: Option[JwtAlgorithm]) =
    new HavePropertyMatcher[JwtHeader, Option[JwtAlgorithm]] {
      def apply(header: JwtHeader) =
        HavePropertyMatchResult(
          header.algorithm == expectedValue,
          "algorithm",
          expectedValue,
          header.algorithm
        )
    }

  def typ(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtHeader, Option[String]] {
      def apply(header: JwtHeader) =
        HavePropertyMatchResult(
          header.typ == expectedValue,
          "typ",
          expectedValue,
          header.typ
        )
    }

  def contentType(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtHeader, Option[String]] {
      def apply(header: JwtHeader) =
        HavePropertyMatchResult(
          header.contentType == expectedValue,
          "contentType",
          expectedValue,
          header.contentType
        )
    }

  def keyId(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtHeader, Option[String]] {
      def apply(header: JwtHeader) =
        HavePropertyMatchResult(
          header.keyId == expectedValue,
          "keyId",
          expectedValue,
          header.keyId
        )
    }

  def testHeader(h1: JwtHeader, h2: JwtHeader) = {
    h1 should have(
      algorithm(h2.algorithm),
      typ(h2.typ),
      contentType(h2.contentType),
      keyId(h2.keyId)
    )
  }

  def testTryHeader(th1: Try[JwtHeader], h2: JwtHeader, clue: String) = {
    withClue(clue) {
      th1 shouldBe a[Success[_]]
      testHeader(th1.get, h2)
    }
  }

  // CLAIM
  def content(expectedValue: String) =
    new HavePropertyMatcher[JwtClaim, String] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.content == expectedValue,
          "content",
          expectedValue,
          claim.content
        )
    }

  def issuer(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtClaim, Option[String]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.issuer == expectedValue,
          "issuer",
          expectedValue,
          claim.issuer
        )
    }

  def subject(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtClaim, Option[String]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.subject == expectedValue,
          "subject",
          expectedValue,
          claim.subject
        )
    }

  def audience(expectedValue: Option[Set[String]]) =
    new HavePropertyMatcher[JwtClaim, Option[Set[String]]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.audience == expectedValue,
          "audience",
          expectedValue,
          claim.audience
        )
    }

  def expiration(expectedValue: Option[Long]) =
    new HavePropertyMatcher[JwtClaim, Option[Long]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.expiration == expectedValue,
          "expiration",
          expectedValue,
          claim.expiration
        )
    }

  def notBefore(expectedValue: Option[Long]) =
    new HavePropertyMatcher[JwtClaim, Option[Long]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.notBefore == expectedValue,
          "notBefore",
          expectedValue,
          claim.notBefore
        )
    }

  def issuedAt(expectedValue: Option[Long]) =
    new HavePropertyMatcher[JwtClaim, Option[Long]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.issuedAt == expectedValue,
          "issuedAt",
          expectedValue,
          claim.issuedAt
        )
    }

  def jwtId(expectedValue: Option[String]) =
    new HavePropertyMatcher[JwtClaim, Option[String]] {
      def apply(claim: JwtClaim) =
        HavePropertyMatchResult(
          claim.jwtId == expectedValue,
          "jwtId",
          expectedValue,
          claim.jwtId
        )
    }

  def testClaim(c1: JwtClaim, c2: JwtClaim) = {
    c1 should have(
      content(c2.content),
      issuer(c2.issuer),
      subject(c2.subject),
      audience(c2.audience),
      expiration(c2.expiration),
      notBefore(c2.notBefore),
      issuedAt(c2.issuedAt),
      jwtId(c2.jwtId)
    )
  }

  def testTryClaim(tc1: Try[JwtClaim], c2: JwtClaim, clue: String) = {
    withClue(clue) {
      tc1 shouldBe a[Success[_]]
      testClaim(tc1.get, c2)
    }
  }

  // ALL
  def testTryAll(
      t: Try[(JwtHeader, JwtClaim, String)],
      exp: (JwtHeader, JwtClaim, String),
      clue: String
  ) = {
    withClue(clue) {
      t shouldBe a[Success[_]]
      val (h1, c1, s1) = t.get
      val (h2, c2, s2) = exp
      testHeader(h1, h2)
      testClaim(c1, c2)
      s1 shouldBe s2
    }
  }

  def testTryAllWithoutSignature(
      t: Try[(JwtHeader, JwtClaim, String)],
      exp: (JwtHeader, JwtClaim),
      clue: String
  ) = {
    withClue(clue) {
      t shouldBe a[Success[_]]
      val (h1, c1, _) = t.get
      val (h2, c2) = exp
      testHeader(h1, h2)
      testClaim(c1, c2)
    }
  }
}
