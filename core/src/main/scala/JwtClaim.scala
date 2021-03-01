package pdi.jwt

import java.time.Clock

case class JwtClaim(
    content: String = "{}",
    issuer: Option[String] = None,
    subject: Option[String] = None,
    audience: Option[Set[String]] = None,
    expiration: Option[Long] = None,
    notBefore: Option[Long] = None,
    issuedAt: Option[Long] = None,
    jwtId: Option[String] = None
) {

  def toJson: String = JwtUtils.mergeJson(
    JwtUtils.hashToJson(
      Seq(
        "iss" -> issuer,
        "sub" -> subject,
        "aud" -> audience.map(set => if (set.size == 1) set.head else set),
        "exp" -> expiration,
        "nbf" -> notBefore,
        "iat" -> issuedAt,
        "jti" -> jwtId
      ).collect { case (key, Some(value)) =>
        key -> value
      }
    ),
    content
  )

  def +(json: String): JwtClaim = copy(content = JwtUtils.mergeJson(this.content, json))

  def +(key: String, value: Any): JwtClaim = copy(
    content = JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(Seq(key -> value)))
  )

  // Ok, it's Any, but just use "primitive" types
  // It will not work with classes or case classes since, you know,
  // there is no way to serialize them to JSON out of the box.
  def ++[T <: Any](fields: (String, T)*): JwtClaim = copy(
    content = JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(fields))
  )

  def by(issuer: String): JwtClaim = copy(issuer = Option(issuer))

  // content should be a valid stringified JSON
  def withContent(content: String): JwtClaim = copy(content = content)

  def to(audience: String): JwtClaim = copy(audience = Option(Set(audience)))

  def to(audience: Set[String]): JwtClaim = copy(audience = Option(audience))

  def about(subject: String): JwtClaim = copy(subject = Option(subject))

  def withId(id: String): JwtClaim = copy(jwtId = Option(id))

  def expiresIn(seconds: Long)(implicit clock: Clock): JwtClaim =
    copy(expiration = Option(JwtTime.nowSeconds + seconds))

  def expiresAt(seconds: Long): JwtClaim = copy(expiration = Option(seconds))

  def expiresNow(implicit clock: Clock): JwtClaim = copy(expiration = Option(JwtTime.nowSeconds))

  def startsIn(seconds: Long)(implicit clock: Clock): JwtClaim =
    copy(notBefore = Option(JwtTime.nowSeconds + seconds))

  def startsAt(seconds: Long): JwtClaim = copy(notBefore = Option(seconds))

  def startsNow(implicit clock: Clock): JwtClaim = copy(notBefore = Option(JwtTime.nowSeconds))

  def issuedIn(seconds: Long)(implicit clock: Clock): JwtClaim =
    copy(issuedAt = Option(JwtTime.nowSeconds + seconds))

  def issuedAt(seconds: Long): JwtClaim = copy(issuedAt = Option(seconds))

  def issuedNow(implicit clock: Clock): JwtClaim = copy(issuedAt = Option(JwtTime.nowSeconds))

  def isValid(issuer: String, audience: String)(implicit clock: Clock): Boolean =
    this.audience.exists(_ contains audience) && this.isValid(issuer)

  def isValid(issuer: String)(implicit clock: Clock): Boolean =
    this.issuer.contains(issuer) && this.isValid

  def isValid(implicit clock: Clock): Boolean =
    JwtTime.nowIsBetweenSeconds(this.notBefore, this.expiration)

}
