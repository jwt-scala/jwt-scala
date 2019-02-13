package pdi.jwt

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
  def toJson: String = JwtUtils.mergeJson(JwtUtils.hashToJson(Seq(
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

  def + (key: String, value: Any): JwtClaim =
    this.copy(content = JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(Seq(key -> value))))

  // Ok, it's Any, but just use "primitive" types
  // It will not work with classes or case classes since, you know,
  // there is no way to serialize them to JSON out of the box.
  def ++ (fields: (String, Any)*): JwtClaim =
    this.copy(content = JwtUtils.mergeJson(this.content, JwtUtils.hashToJson(fields)))

  def by(issuer: String): JwtClaim = this.copy(issuer = Option(issuer))
  def to(audience: String): JwtClaim = this.copy(audience = Some(Set(audience)))
  def to(audience: Set[String]): JwtClaim = this.copy(audience = Some(audience))
  def about(subject: String): JwtClaim = this.copy(subject = Option(subject))
  def withId(id: String): JwtClaim = this.copy(jwtId = Option(id))

  def expiresIn(seconds: Long): JwtClaim = this.copy(expiration = Option(JwtTime.nowSeconds + seconds))
  def expiresAt(seconds: Long): JwtClaim = this.copy(expiration = Option(seconds))
  def expiresNow: JwtClaim = this.copy(expiration = Option(JwtTime.nowSeconds))

  def startsIn(seconds: Long): JwtClaim = this.copy(notBefore = Option(JwtTime.nowSeconds + seconds))
  def startsAt(seconds: Long): JwtClaim = this.copy(notBefore = Option(seconds))
  def startsNow: JwtClaim = this.copy(notBefore = Option(JwtTime.nowSeconds))

  def issuedIn(seconds: Long): JwtClaim = this.copy(issuedAt = Option(JwtTime.nowSeconds + seconds))
  def issuedAt(seconds: Long): JwtClaim = this.copy(issuedAt = Option(seconds))
  def issuedNow: JwtClaim = this.copy(issuedAt = Option(JwtTime.nowSeconds))

  def isValid: Boolean = JwtTime.nowIsBetweenSeconds(this.notBefore, this.expiration)
  def isValid(issuer: String): Boolean = this.issuer.exists(_ == issuer) && this.isValid
  def isValid(issuer: String, audience: String): Boolean = this.audience.exists(_ contains audience) && this.isValid(issuer)
}
