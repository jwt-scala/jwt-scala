package pdi.jwt

import java.time.Clock

import pdi.jwt.exceptions._

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
  ) = new JwtClaim(
    ujson.read(content) match {
      case obj: ujson.Obj => obj
      case _              => throw new JwtInvalidJsonException()
    },
    issuer,
    subject,
    audience,
    expiration,
    notBefore,
    issuedAt,
    jwtId
  )
}

class JwtClaim(
    val content: ujson.Obj,
    val issuer: Option[String],
    val subject: Option[String],
    val audience: Option[Set[String]],
    val expiration: Option[Long],
    val notBefore: Option[Long],
    val issuedAt: Option[Long],
    val jwtId: Option[String]
) {

  def toJson: String = ujson.write(
    ujson.Obj.from(
      content.value ++ Seq(
        "iss" -> issuer.map(ujson.Str),
        "sub" -> subject.map(ujson.Str),
        "aud" -> audience.map(set => if (set.size == 1) ujson.Str(set.head) else ujson.Arr(set)),
        "exp" -> expiration.map(e => ujson.Num(e.toDouble)),
        "nbf" -> notBefore.map(nbf => ujson.Num(nbf.toDouble)),
        "iat" -> issuedAt.map(e => ujson.Num(e.toDouble)),
        "jti" -> jwtId.map(ujson.Str)
      ).collect { case (key, Some(value)) =>
        key -> value
      }
    )
  )

  def +(json: String): JwtClaim = {
    new JwtClaim(
      ujson.Obj.from(this.content.value ++ ujson.read(json).obj),
      issuer,
      subject,
      audience,
      expiration,
      notBefore,
      issuedAt,
      jwtId
    )
  }

  def +(key: String, value: Any): JwtClaim = {
    JwtClaim(
      JwtUtils.mergeJson(ujson.write(this.content), JwtUtils.hashToJson(Seq(key -> value))),
      issuer,
      subject,
      audience,
      expiration,
      notBefore,
      issuedAt,
      jwtId
    )
  }

  // Ok, it's Any, but just use "primitive" types
  // It will not work with classes or case classes since, you know,
  // there is no way to serialize them to JSON out of the box.
  def ++[T <: Any](fields: (String, T)*): JwtClaim = {
    JwtClaim(
      JwtUtils.mergeJson(ujson.write(this.content), JwtUtils.hashToJson(fields)),
      issuer,
      subject,
      audience,
      expiration,
      notBefore,
      issuedAt,
      jwtId
    )
  }

  def by(issuer: String): JwtClaim = {
    new JwtClaim(content, Option(issuer), subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  // content should be a valid stringified JSON
  def withContent(content: String): JwtClaim = {
    JwtClaim(content, issuer, subject, audience, expiration, notBefore, issuedAt, jwtId)
  }

  def to(audience: String): JwtClaim = {
    new JwtClaim(
      content,
      issuer,
      subject,
      Option(Set(audience)),
      expiration,
      notBefore,
      issuedAt,
      jwtId
    )
  }

  def to(audience: Set[String]): JwtClaim = {
    new JwtClaim(content, issuer, subject, Option(audience), expiration, notBefore, issuedAt, jwtId)
  }

  def about(subject: String): JwtClaim = {
    new JwtClaim(content, issuer, Option(subject), audience, expiration, notBefore, issuedAt, jwtId)
  }

  def withId(id: String): JwtClaim = {
    new JwtClaim(content, issuer, subject, audience, expiration, notBefore, issuedAt, Option(id))
  }

  def expiresAt(seconds: Long): JwtClaim =
    new JwtClaim(content, issuer, subject, audience, Option(seconds), notBefore, issuedAt, jwtId)

  def expiresIn(seconds: Long)(implicit clock: Clock): JwtClaim = expiresAt(
    JwtTime.nowSeconds + seconds
  )

  def expiresNow(implicit clock: Clock): JwtClaim = expiresAt(JwtTime.nowSeconds)

  def startsAt(seconds: Long): JwtClaim =
    new JwtClaim(content, issuer, subject, audience, expiration, Option(seconds), issuedAt, jwtId)

  def startsIn(seconds: Long)(implicit clock: Clock): JwtClaim = startsAt(
    JwtTime.nowSeconds + seconds
  )

  def startsNow(implicit clock: Clock): JwtClaim = startsAt(JwtTime.nowSeconds)

  def issuedAt(seconds: Long): JwtClaim =
    new JwtClaim(content, issuer, subject, audience, expiration, notBefore, Option(seconds), jwtId)

  def issuedIn(seconds: Long)(implicit clock: Clock): JwtClaim = issuedAt(
    JwtTime.nowSeconds + seconds
  )

  def issuedNow(implicit clock: Clock): JwtClaim = issuedAt(JwtTime.nowSeconds)

  def isValid(issuer: String, audience: String)(implicit clock: Clock): Boolean =
    this.audience.exists(_ contains audience) && this.isValid(issuer)

  def isValid(issuer: String)(implicit clock: Clock): Boolean =
    this.issuer.contains(issuer) && this.isValid

  def isValid(implicit clock: Clock): Boolean =
    JwtTime.nowIsBetweenSeconds(this.notBefore, this.expiration)

  // equality code
  def canEqual(other: Any): Boolean = other.isInstanceOf[JwtClaim]

  override def equals(other: Any): Boolean = other match {
    case that: JwtClaim =>
      (that.canEqual(this)) &&
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

  override def toString: String =
    s"JwtClaim($content, $issuer, $subject, $audience, $expiration, $notBefore, $issuedAt, $jwtId)"
}
