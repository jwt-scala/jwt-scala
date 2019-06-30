package pdi.jwt

import java.time.Clock

object JwtClaim {
  def apply(
    content: String = "{}",
    issuer: Option[String] = None,
    subject: Option[String] = None,
    audience: Option[Set[String]] = None,
    expiration: Option[Long] = None,
    notBefore: Option[Long] = None,
    issuedAt: Option[Long] = None,
    jwtId: Option[String] = None
  ) = new JwtClaim(content, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
}

class JwtClaim(
  val content: String,
  val issuer: Option[String],
  val subject: Option[String],
  val audience: Option[Set[String]],
  val expiration: Option[Long],
  val notBefore: Option[Long],
  val issuedAt: Option[Long],
  val jwtId: Option[String]
) {

  def toJson: String = JwtUtils.mergeJson(JwtUtils.hashToJson(Seq(
    "iss" -> issuer,
    "sub" -> subject,
    "aud" -> audience.map(set => if (set.size == 1) set.head else set),
    "exp" -> expiration,
    "nbf" -> notBefore,
    "iat" -> issuedAt,
    "jti" -> jwtId
  ).collect {
    case (key, Some(value)) => key -> value
  }), content)

  def +(json: String): JwtClaim = {
    JwtClaim(JwtUtils.mergeJson(this.content, json), issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  def +(key: String, value: Any): JwtClaim = {
    JwtClaim(JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(Seq(key -> value))), issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  // Ok, it's Any, but just use "primitive" types
  // It will not work with classes or case classes since, you know,
  // there is no way to serialize them to JSON out of the box.
  def ++[T <: Any](fields: (String, T)*): JwtClaim = {
    JwtClaim(JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(fields)), issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  def by(issuer: String): JwtClaim = {
    JwtClaim(content, Option(issuer), subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  // content should be a valid stringified JSON
  def withContent(content: String): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  def to(audience: String): JwtClaim = {
    JwtClaim(content, issuer, subject, Option(Set(audience)), expiration, notBefore, issuedAt, jwtId)
  }

  def to(audience: Set[String]): JwtClaim = {
    JwtClaim(content, issuer, subject, Option(audience), expiration, notBefore, issuedAt, jwtId)
  }

  def about(subject: String): JwtClaim = {
    JwtClaim(content, issuer, Option(subject), audience, expiration, notBefore, issuedAt, jwtId)
  }

  def withId(id: String): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, issuedAt, Option(id))
  }

  def expiresIn(seconds: Long)(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, Option(JwtTime.nowSeconds + seconds), notBefore, issuedAt, jwtId)
  }

  def expiresAt(seconds: Long): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, Option(seconds), notBefore, issuedAt, jwtId)
  }

  def expiresNow(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, Option(JwtTime.nowSeconds), notBefore, issuedAt, jwtId)
  }

  def startsIn(seconds: Long)(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, Option(JwtTime.nowSeconds + seconds), issuedAt, jwtId)
  }

  def startsAt(seconds: Long): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, Option(seconds), issuedAt, jwtId)
  }

  def startsNow(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, Option(JwtTime.nowSeconds), issuedAt, jwtId)
  }

  def issuedIn(seconds: Long)(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, Option(JwtTime.nowSeconds + seconds), jwtId)
  }

  def issuedAt(seconds: Long): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, Option(seconds), jwtId)
  }

  def issuedNow(implicit clock: Clock): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, Option(JwtTime.nowSeconds), jwtId)
  }

  def isValid(issuer: String, audience: String)(implicit clock: Clock): Boolean = this.audience.exists(_ contains audience) && this.isValid(issuer)

  def isValid(issuer: String)(implicit clock: Clock): Boolean = this.issuer.contains(issuer) && this.isValid

  def isValid(implicit clock: Clock): Boolean = JwtTime.nowIsBetweenSeconds(this.notBefore, this.expiration)

  // equality code
  def canEqual(other: Any): Boolean = other.isInstanceOf[JwtClaim]

  override def equals(other: Any): Boolean = other match {
    case that: JwtClaim =>
      (that canEqual this) &&
        content == that.content &&
        issuer == that.issuer &&
        subject == that.subject &&
        audience == that.audience &&
        expiration == that.expiration &&
        notBefore == that.notBefore &&
        issuedAt == that.issuedAt &&
        jwtId == that.jwtId
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(content, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
