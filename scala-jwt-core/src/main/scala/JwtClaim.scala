package pdi.scala.jwt

case class JwtClaim(
  content: String = "{}",
  issuer: Option[String] = None,
  subject: Option[String] = None,
  audience: Option[String] = None,
  expiration: Option[Long] = None,
  notBefore: Option[Long] = None,
  issuedAt: Option[Long] = None,
  jwtId: Option[String] = None
) {
  def toJson: String = JwtUtils.mergeJson(JwtUtils.seqToJson(Seq(
    "iss" -> issuer,
    "sub" -> subject,
    "aud" -> audience,
    "exp" -> expiration,
    "nbf" -> notBefore,
    "iat" -> issuedAt,
    "jti" -> jwtId
  ).collect {
    case (key, Some(value)) => (key -> value)
  }), content)

  def + (json: String): JwtClaim = this.copy(content = JwtUtils.mergeJson(this.content, json))

  def + (fields: (String, Any)*): JwtClaim =
    this.copy(content = JwtUtils.mergeJson(this.content, JwtUtils.seqToJson(fields)))

  def by(issuer: String): JwtClaim = this.copy(issuer = Option(issuer))
  def to(audience: String): JwtClaim = this.copy(audience = Option(audience))
  def about(subject: String): JwtClaim = this.copy(subject = Option(subject))
  def withId(id: String): JwtClaim = this.copy(jwtId = Option(id))

    /*this.copy(expiration = this.expiration.map(_ + millis).orElse(Option(JwtTime.now + millis)))*/
  def expiresIn(millis: Long): JwtClaim = this.copy(expiration = Option(JwtTime.now + millis))
  def expiresAt(millis: Long): JwtClaim = this.copy(expiration = Option(millis))
  def expiresNow: JwtClaim = this.copy(expiration = Option(JwtTime.now))

  def startsIn(millis: Long): JwtClaim = this.copy(notBefore = Option(JwtTime.now + millis))
  def startsAt(millis: Long): JwtClaim = this.copy(notBefore = Option(millis))
  def startsNow: JwtClaim = this.copy(notBefore = Option(JwtTime.now))

  def issuedIn(millis: Long): JwtClaim = this.copy(issuedAt = Option(JwtTime.now + millis))
  def issuedAt(millis: Long): JwtClaim = this.copy(issuedAt = Option(millis))
  def issuedNow: JwtClaim = this.copy(issuedAt = Option(JwtTime.now))

  def isValid: Boolean = JwtTime.nowIsBetween(this.notBefore, this.expiration)
  def isValid(issuer: String): Boolean = issuer == this.issuer && this.isValid
  def isValid(issuer: String, audience: String): Boolean = audience == this.audience && this.isValid(issuer)
}
