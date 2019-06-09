## Jwt Reserved Claims and Private Claims

A common use-case of Jwt-Scala (and JWT at large) is developing so-called "public" or "private" claims (and or header params).  These are functionally no different than "reserved" claims/header params, other than that they have no standard definition and may only be distinguished within your network or niche industry. "issuer", "subject", "audience" etc. are all examples of reserved claims, whereas "user" is a fairly common example of a non-reserved claim.

Given that there may be many of these public/private claims, rather than parsing them yourself separate from how reserved claims are parsed (see the *JwtClaim Class*), you can simply compose `JwtClaim` with your own custom claims that extend from the `JwtReservedClaim` trait.

Here is an example where reserved headers, along with a private "user" claim, is used:

```tut
import pdi.jwt.{Jwt, JwtHeader, JwtClaim, JwtUtils, JwtJson4sParser}
// define your network-specific claims, and compose them with the usual reservedClaims
case class JwtPrivateClaim(user: Option[String] = None, reservedClaims: JwtClaim = JwtClaim()) {
  // merge your json definition along with the reserved claims too
  def toJson: String = JwtUtils.mergeJson(JwtUtils.hashToJson(Seq(
      "user" -> user,
    ).collect {
      case (key, Some(value)) => (key -> value)
    }), reservedClaims.toJson)
}

// create a parser with claim type set to the one you just defined
// notice that the default `JwtHeader` class was used since we're only interested in overriding with a custom private claims type in this example
object JwtJson4sPrivate extends JwtJson4sParser[JwtHeader, JwtPrivateClaim] {
  override protected def parseClaim(claim: String): JwtPrivateClaim = {
    val claimJson = super.parse(claim)
    val jwtReservedClaim: JwtClaim = super.readClaim(claimJson)
    val content = super.parse(jwtReservedClaim.content)
    JwtPrivateClaim(super.extractString(content, "user"), jwtReservedClaim.withContent("{}"))
  }

  // here is the only boilerplate (but if you chose to also specify a custom header type then you would make use of this)
  override protected def parseHeader(header: String): JwtHeader = super.readHeader(parse(header))

  // marginal boilerplate to ensure consistency with isValid checks now that your nesting reserved claims into your custom private claims
  override protected def extractExpiration(claim: JwtPrivateClaim): Option[Long] = claim.reservedClaims.expiration
  override protected def extractNotBefore(claim: JwtPrivateClaim): Option[Long] = claim.reservedClaims.notBefore
}
```

You can then use the same decodeAll method as you would before, now with your fully objectified claims:

```tut
import scala.util.Try
// this example chose to use JwtJson4s, but any Json implementation would work the same
val token: String = Jwt.encode("""{"user":"someone", "iss": "me"}""");
val decoded: Try[(JwtHeader, JwtPrivateClaim, String)] = JwtJson4sPrivate.decodeAll(token)
```
