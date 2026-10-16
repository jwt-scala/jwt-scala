package pdi.jwt

import java.security.interfaces.RSAPublicKey
import java.security.{MessageDigest, PublicKey}

import pdi.jwt.exceptions.JwtLengthException

/** A JWK thumbprint as defined by [[https://www.rfc-editor.org/rfc/rfc7638 RFC 7638]].
  *
  * The thumbprint is the digest itself: the hash octets of the canonical JSON form of the key.
  * [[base64Url]] is its textual form (unpadded base64url), which is the value conventionally used
  * as a JWT/JWK `kid`. [[uri]] is the RFC 9278 URI form.
  *
  * Only SHA-256 is supported at the moment. RFC 7638 permits other hash functions.
  */
final class JwkThumbprint private (val base64Url: String) {
  {
    val length = JwtBase64.decode(base64Url).length
    if (length != JwkThumbprint.Sha256Length)
      throw new JwtLengthException(
        s"A SHA-256 thumbprint must be ${JwkThumbprint.Sha256Length} bytes, got $length."
      )
  }

  /** The digest octets. A fresh array is returned on every call. */
  def toBytes: Array[Byte] = JwtBase64.decode(base64Url)

  /** The RFC 9278 JWK thumbprint URI, e.g. `urn:ietf:params:oauth:jwk-thumbprint:sha-256:<digest>`.
    */
  def uri: String = JwkThumbprint.UriPrefix + base64Url

  override def equals(other: Any): Boolean = other match {
    case that: JwkThumbprint => base64Url == that.base64Url
    case _                   => false
  }

  override def hashCode(): Int = base64Url.hashCode()

  override def toString: String = base64Url
}

object JwkThumbprint {
  private val UriPrefix = "urn:ietf:params:oauth:jwk-thumbprint:sha-256:"
  private val Sha256Length = 32

  def apply(base64Url: String): JwkThumbprint = new JwkThumbprint(base64Url)

  /** Compute the SHA-256 JWK thumbprint of a key (RFC 7638 section 3, RFC 8037 section 2).
    *
    * The digest is taken over the UTF-8 encoding of the canonical JSON made of the required members
    * of the key in lexicographic order and without whitespace, e.g. `{"e":..,"kty":"RSA","n":..}`.
    */
  def sha256(jwk: Jwk): JwkThumbprint =
    JwkThumbprint(
      JwtBase64.encodeString(
        MessageDigest
          .getInstance("SHA-256")
          .digest(JwtUtils.bytify(JwtUtils.hashToJson(jwk.thumbprintMembers)))
      )
    )

  def sha256(key: RSAPublicKey): JwkThumbprint = sha256(Jwk.fromPublicKey(key))

  /** @throws pdi.jwt.exceptions.JwtNonSupportedAlgorithm
    *   if the key is not an RSA, EC or Ed25519 / Ed448 key
    * @throws pdi.jwt.exceptions.JwtNonSupportedCurve
    *   if the key is an EC key on an unsupported curve
    */
  def sha256(key: PublicKey): JwkThumbprint = sha256(Jwk.fromPublicKey(key))

  /** Parse the unpadded base64url form of a SHA-256 thumbprint. The digest is re-encoded, so that
    * two spellings of the same digest give equal thumbprints.
    *
    * @throws pdi.jwt.exceptions.JwtLengthException
    *   if the value does not decode to 32 bytes
    * @throws java.lang.IllegalArgumentException
    *   if the value is not valid base64url
    */
  def fromBase64Url(value: String): JwkThumbprint =
    JwkThumbprint(JwtBase64.encodeString(JwtBase64.decode(value)))
}
