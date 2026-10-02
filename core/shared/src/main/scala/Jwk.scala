package pdi.jwt

import java.math.BigInteger
import java.security.interfaces.{ECPublicKey, RSAPublicKey}
import java.security.spec.{ECFieldFp, ECPoint, ECPublicKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PublicKey}
import scala.annotation.nowarn

import pdi.jwt.algorithms.*
import pdi.jwt.exceptions.*

/** A public JSON Web Key as defined by [[https://www.rfc-editor.org/rfc/rfc7517 RFC 7517]].
  *
  * Only the public parameters of `RSA`, `EC` and `OKP` keys are modelled. Private members such as
  * `d`, `p`, `q`, `dp`, `dq`, `qi` or `oth` are ignored: they are never read when parsing and never
  * written by `toJson`.
  *
  * The certificate members `x5c`, `x5t`, `x5t#S256` and `x5u` are ignored too: no certificate chain
  * is read, validated or checked against the key, and they are never written by `toJson`.
  */
sealed trait Jwk {

  /** The `kid` member. */
  def keyId: Option[String]

  /** The `use` member. */
  def use: Option[JwkUse]

  /** The raw `alg` member, which may name a non-signature algorithm. */
  def algorithm: Option[String]

  /** The `key_ops` member, `None` when absent. */
  def keyOperations: Option[Set[String]]

  /** @throws java.security.spec.InvalidKeySpecException
    *   if the JDK refuses the parameters as a key
    * @throws java.security.NoSuchAlgorithmException
    *   if the JDK has no key factory for this type of key
    */
  def publicKey: PublicKey

  /** The SHA-256 thumbprint of this key, see [[JwkThumbprint]]. */
  def thumbprint: JwkThumbprint = JwkThumbprint.sha256(this)

  def toJson: String = JwtUtils.hashToJson(
    Seq[(String, Option[Any])](
      "kty" -> Option(keyType),
      "kid" -> keyId,
      "use" -> use.map(_.value),
      "alg" -> algorithm,
      "key_ops" -> keyOperations.map(_.toSeq.sorted)
    ).collect { case (name, Some(value)) => name -> value } ++ keyMembers
  )

  private[jwt] def keyType: String

  /** The key parameters, as JSON string members in `toJson` order. */
  private[jwt] def keyMembers: Seq[(String, String)]

  /** The members of the RFC 7638 canonical form, in lexicographic order. */
  private[jwt] def thumbprintMembers: Seq[(String, String)] =
    (("kty" -> keyType) +: keyMembers).sortBy(_._1)

  /** Whether a token signed with this algorithm can be verified with this key. */
  private[jwt] def isCompatibleWith(algorithm: JwtAsymmetricAlgorithm): Boolean
}

object Jwk {

  /** Build a JWK from a JDK public key.
    *
    * @throws JwtNonSupportedAlgorithm
    *   if the key is not an RSA, EC or Ed25519 / Ed448 key
    * @throws JwtNonSupportedCurve
    *   if the key is an EC key on a curve other than P-256, P-384 or P-521
    */
  def fromPublicKey(
      key: PublicKey,
      keyId: Option[String] = None,
      use: Option[JwkUse] = None,
      algorithm: Option[String] = None,
      keyOperations: Option[Set[String]] = None
  ): Jwk = key match {
    case rsa: RSAPublicKey =>
      RsaJwk(keyId, use, algorithm, rsa.getModulus, rsa.getPublicExponent, keyOperations)
    case ec: ECPublicKey =>
      val curve = EcCurve
        .of(ec.getParams)
        .getOrElse(throw new JwtNonSupportedCurve(ec.getParams.toString))
      EcJwk(keyId, use, algorithm, curve, ec.getW.getAffineX, ec.getW.getAffineY, keyOperations)
    case other =>
      val encoded = Option(other.getEncoded).getOrElse(Array.emptyByteArray)
      val curve = OkpCurve
        .all()
        .find { curve =>
          val header = curve.x509Header
          encoded.length == header.length + curve.keyLength &&
          encoded.take(header.length).sameElements(header)
        }
        .getOrElse(throw new JwtNonSupportedAlgorithm(other.getAlgorithm))
      OkpJwk(
        keyId,
        use,
        algorithm,
        curve,
        JwtBase64.encodeString(encoded.drop(curve.x509Header.length)),
        keyOperations
      )
  }

  /** Build a JWK from its members, which JSON modules read from their own representation.
    *
    * `member` returns the string value of a member, or `None` when it is absent, and throws
    * `JwtNonStringException` when it is present with another type. `listMember` does the same for a
    * member that is an array of strings. They are only called for the members relevant to the type
    * of key.
    */
  private[jwt] def fromMembers(
      member: String => Option[String],
      listMember: String => Option[List[String]]
  ): Jwk = {
    def required(name: String): String =
      member(name).getOrElse(throw new JwtNonStringException(name))

    val keyId = member("kid")
    val use = member("use").map(JwkUse.fromString)
    val algorithm = member("alg")
    val keyOperations = listMember("key_ops").map(_.toSet)

    required("kty") match {
      case "RSA" =>
        RsaJwk(
          keyId,
          use,
          algorithm,
          JwtUtils.decodeUInt("n", required("n")),
          JwtUtils.decodeUInt("e", required("e")),
          keyOperations
        )
      case "EC" =>
        val crv = required("crv")
        val curve = EcCurve.fromName(crv).getOrElse(throw new JwtNonSupportedCurve(crv))
        EcJwk(
          keyId,
          use,
          algorithm,
          curve,
          new BigInteger(1, JwtUtils.decodeFixed("x", required("x"), curve.coordinateLength)),
          new BigInteger(1, JwtUtils.decodeFixed("y", required("y"), curve.coordinateLength)),
          keyOperations
        )
      case "OKP" =>
        val crv = required("crv")
        val curve = OkpCurve.fromName(crv).getOrElse(throw new JwtNonSupportedCurve(crv))
        OkpJwk(
          keyId,
          use,
          algorithm,
          curve,
          JwtBase64.encodeString(JwtUtils.decodeFixed("x", required("x"), curve.keyLength)),
          keyOperations
        )
      case other => throw new JwtNonSupportedAlgorithm(other)
    }
  }
}

final case class RsaJwk(
    keyId: Option[String],
    use: Option[JwkUse],
    algorithm: Option[String],
    modulus: BigInteger,
    exponent: BigInteger,
    keyOperations: Option[Set[String]] = None
) extends Jwk {

  lazy val publicKey: RSAPublicKey = JwtUtils.rsaPublicKey(modulus, exponent)

  /** The `n` member: the modulus as unsigned big-endian base64url. */
  lazy val n: String = JwtUtils.encodeUInt(modulus)

  /** The `e` member: the public exponent as unsigned big-endian base64url. */
  lazy val e: String = JwtUtils.encodeUInt(exponent)

  private[jwt] def keyType = "RSA"

  private[jwt] def keyMembers = Seq("n" -> n, "e" -> e)

  private[jwt] def isCompatibleWith(algorithm: JwtAsymmetricAlgorithm): Boolean =
    algorithm.isInstanceOf[JwtRSAAlgorithm]
}

/** @throws JwtValidationException
  *   if a coordinate does not fit the curve length, is negative, or the point is not on the curve
  */
final case class EcJwk(
    keyId: Option[String],
    use: Option[JwkUse],
    algorithm: Option[String],
    curve: EcCurve,
    xCoordinate: BigInteger,
    yCoordinate: BigInteger,
    keyOperations: Option[Set[String]] = None
) extends Jwk {
  JwtUtils.requireFits(xCoordinate, curve.coordinateLength)
  JwtUtils.requireFits(yCoordinate, curve.coordinateLength)
  if (!isOnCurve) throw new JwtValidationException("The point is not on the curve.")

  /** The `x` member: the coordinate as base64url of exactly `curve.coordinateLength` bytes. */
  lazy val x: String = JwtUtils.encodeFixed(xCoordinate, curve.coordinateLength)

  /** The `y` member: the coordinate as base64url of exactly `curve.coordinateLength` bytes. */
  lazy val y: String = JwtUtils.encodeFixed(yCoordinate, curve.coordinateLength)

  lazy val publicKey: ECPublicKey =
    KeyFactory
      .getInstance("EC")
      .generatePublic(
        new ECPublicKeySpec(new ECPoint(xCoordinate, yCoordinate), curve.parameterSpec)
      )
      .asInstanceOf[ECPublicKey]

  private def isOnCurve: Boolean = {
    val ecCurve = curve.parameterSpec.getCurve
    val p = ecCurve.getField.asInstanceOf[ECFieldFp].getP
    val inRange = xCoordinate.compareTo(p) < 0 && yCoordinate.compareTo(p) < 0
    inRange && yCoordinate.multiply(yCoordinate).mod(p) == xCoordinate
      .pow(3)
      .add(ecCurve.getA.multiply(xCoordinate))
      .add(ecCurve.getB)
      .mod(p)
  }

  private[jwt] def keyType = "EC"

  private[jwt] def keyMembers = Seq("crv" -> curve.name, "x" -> x, "y" -> y)

  private[jwt] def isCompatibleWith(algorithm: JwtAsymmetricAlgorithm): Boolean =
    (algorithm, curve) match {
      case (JwtAlgorithm.ES256, EcCurve.P256) => true
      case (JwtAlgorithm.ES384, EcCurve.P384) => true
      case (JwtAlgorithm.ES512, EcCurve.P521) => true
      case _                                  => false
    }
}

/** An `OKP` JWK (RFC 8037) holding an Ed25519 or Ed448 public key.
  *
  * Building the `publicKey` of such a key requires a JDK that provides the `EdDSA` `KeyFactory`,
  * which is the case from JDK 15 on. See [[OkpCurve]].
  *
  * @param x
  *   the `x` member: the raw public key as unpadded base64url
  * @throws JwtValidationException
  *   if `x` does not decode to `curve.keyLength` bytes
  * @throws java.lang.IllegalArgumentException
  *   if `x` is not valid base64url
  */
final case class OkpJwk(
    keyId: Option[String],
    use: Option[JwkUse],
    algorithm: Option[String],
    curve: OkpCurve,
    x: String,
    keyOperations: Option[Set[String]] = None
) extends Jwk {
  JwtUtils.requireDecodedLength("x", x, curve.keyLength)

  /** The raw public key. A fresh array is returned on every call. */
  def keyBytes: Array[Byte] = JwtBase64.decode(x)

  lazy val publicKey: PublicKey =
    KeyFactory
      .getInstance(JwtUtils.EdDSA)
      .generatePublic(new X509EncodedKeySpec(curve.x509Header ++ keyBytes))

  private[jwt] def keyType = "OKP"

  private[jwt] def keyMembers = Seq("crv" -> curve.name, "x" -> x)

  @nowarn("cat=deprecation")
  private[jwt] def isCompatibleWith(algorithm: JwtAsymmetricAlgorithm): Boolean =
    (algorithm, curve) match {
      case (JwtAlgorithm.EdDSA, _)                  => true
      case (JwtAlgorithm.Ed25519, OkpCurve.Ed25519) => true
      case _                                        => false
    }
}
