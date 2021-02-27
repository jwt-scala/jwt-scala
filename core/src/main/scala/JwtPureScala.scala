package pdi.jwt

import scala.util.matching.Regex
import java.time.Clock

/** Test implementation of [[JwtCore]] using only Strings. Most of the time, you should use a lib
  * implementing JSON and shouldn't be using this object. But just in case you need pure Scala support,
  * here it is.
  *
  * To see a full list of samples, check the [[http://pauldijou.fr/jwt-scala/samples/jwt-core/ online documentation]].
  *
  * '''Warning''': since there is no JSON support in Scala, this object doesn't have any way to parse
  * a JSON string as an AST, so it only uses regex with all the limitations it implies. Try not to use
  * keys like `exp` and `nbf` in sub-objects of the claim. For example, if you try to use the following
  * claim: `{"user":{"exp":1},"exp":1300819380}`, it should be correct but it will fail because the regex
  * extracting the expiration will return `1` instead of `1300819380`. Sorry about that.
  */
object Jwt extends JwtCore[JwtHeader, JwtClaim] {
  def apply(clock: Clock): Jwt = new Jwt(clock)

  private val extractAlgorithmRegex = "\"alg\" *: *\"([a-zA-Z0-9]+)\"".r
  protected def extractAlgorithm(header: String): Option[JwtAlgorithm] =
    (extractAlgorithmRegex findFirstMatchIn header).map(_.group(1)).flatMap {
      case "none"       => None
      case name: String => Some(JwtAlgorithm.fromString(name))
    }

  private val extractIssuerRegex = "\"iss\" *: *\"([a-zA-Z0-9]*)\"".r
  protected def extractIssuer(claim: String): Option[String] =
    (extractIssuerRegex findFirstMatchIn claim).map(_.group(1))

  private val extractSubjectRegex = "\"sub\" *: *\"([a-zA-Z0-9]*)\"".r
  protected def extractSubject(claim: String): Option[String] =
    (extractSubjectRegex findFirstMatchIn claim).map(_.group(1))

  private val extractExpirationRegex = "\"exp\" *: *([0-9]+)".r
  protected def extractExpiration(claim: String): Option[Long] =
    (extractExpirationRegex findFirstMatchIn claim).map(_.group(1)).map(_.toLong)

  private val extractNotBeforeRegex = "\"nbf\" *: *([0-9]+)".r
  protected def extractNotBefore(claim: String): Option[Long] =
    (extractNotBeforeRegex findFirstMatchIn claim).map(_.group(1)).map(_.toLong)

  private val extractIssuedAtRegex = "\"iat\" *: *([0-9]+)".r
  protected def extractIssuedAt(claim: String): Option[Long] =
    (extractIssuedAtRegex findFirstMatchIn claim).map(_.group(1)).map(_.toLong)

  private val extractJwtIdRegex = "\"jti\" *: *\"([a-zA-Z0-9]*)\"".r
  protected def extractJwtId(claim: String): Option[String] =
    (extractJwtIdRegex findFirstMatchIn claim).map(_.group(1))

  private val clearStartRegex = "\\{ *,".r
  protected def clearStart(json: String): String =
    clearStartRegex.replaceFirstIn(json, "{")

  private val clearMiddleRegex = ", *(?=,)".r
  protected def clearMiddle(json: String): String =
    clearMiddleRegex.replaceAllIn(json, "")

  private val clearEndRegex = ", *\\}".r
  protected def clearEnd(json: String): String =
    clearEndRegex.replaceFirstIn(json, "}")

  protected def clearRegex(json: String, regex: Regex): String =
    regex.replaceFirstIn(json, "")

  protected def clearAll(json: String): String = {
    val dirtyJson = List(
      extractIssuerRegex,
      extractSubjectRegex,
      extractExpirationRegex,
      extractNotBeforeRegex,
      extractIssuedAtRegex,
      extractJwtIdRegex
    ).foldLeft(json)(clearRegex)

    clearStart(clearEnd(clearMiddle(dirtyJson)))
  }

  protected def parseHeader(header: String): JwtHeader = JwtHeader(extractAlgorithm(header))

  protected def parseClaim(claim: String): JwtClaim =
    JwtClaim(
      content = clearAll(claim),
      issuer = extractIssuer(claim),
      subject = extractSubject(claim),
      expiration = extractExpiration(claim),
      notBefore = extractNotBefore(claim),
      issuedAt = extractIssuedAt(claim),
      jwtId = extractJwtId(claim)
    )

  protected def headerToJson(header: JwtHeader): String = header.toJson
  protected def claimToJson(claim: JwtClaim): String = claim.toJson

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore
}

class Jwt private (override val clock: Clock) extends JwtCore[JwtHeader, JwtClaim] {
  protected def parseHeader(header: String): JwtHeader = Jwt.parseHeader(header)
  protected def parseClaim(claim: String): JwtClaim = Jwt.parseClaim(claim)

  protected def extractAlgorithm(header: JwtHeader): Option[JwtAlgorithm] = header.algorithm
  protected def extractExpiration(claim: JwtClaim): Option[Long] = claim.expiration
  protected def extractNotBefore(claim: JwtClaim): Option[Long] = claim.notBefore

  protected def headerToJson(header: JwtHeader): String = header.toJson
  protected def claimToJson(claim: JwtClaim): String = claim.toJson
}
