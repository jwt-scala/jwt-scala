package pdi.jwt

import scala.annotation.nowarn

import pdi.jwt.algorithms.JwtUnknownAlgorithm

sealed trait JwtAlgorithm {
  def name: String
  def fullName: String
}

package algorithms {
  sealed trait JwtAsymmetricAlgorithm extends JwtAlgorithm

  sealed trait JwtHmacAlgorithm extends JwtAlgorithm
  sealed trait JwtRSAAlgorithm extends JwtAsymmetricAlgorithm
  sealed trait JwtECDSAAlgorithm extends JwtAsymmetricAlgorithm
  sealed trait JwtEdDSAAlgorithm extends JwtAsymmetricAlgorithm
  final case class JwtUnknownAlgorithm(name: String) extends JwtAlgorithm {
    def fullName: String = name
  }
}

object JwtAlgorithm {

  /** Deserialize an algorithm from its string equivalent. Only real algorithms supported, if you
    * need to support "none", use "optionFromString".
    *
    * @return
    *   the actual instance of the algorithm
    * @param algo
    *   the name of the algorithm (e.g. HS256 or HmacSHA256)
    * @throws JwtNonSupportedAlgorithm
    *   in case the string doesn't match any known algorithm
    */
  @nowarn("cat=deprecation")
  def fromString(algo: String): JwtAlgorithm = algo match {
    case "HMD5"    => HMD5
    case "HS224"   => HS224
    case "HS256"   => HS256
    case "HS384"   => HS384
    case "HS512"   => HS512
    case "RS256"   => RS256
    case "RS384"   => RS384
    case "RS512"   => RS512
    case "ES256"   => ES256
    case "ES384"   => ES384
    case "ES512"   => ES512
    case "EdDSA"   => EdDSA
    case "Ed25519" => Ed25519
    case other     => JwtUnknownAlgorithm(other)
    // Missing PS256 PS384 PS512
  }

  /** Deserialize an algorithm from its string equivalent. If it's the special "none" algorithm,
    * return None, else, return Some with the corresponding algorithm inside.
    *
    * @return
    *   the actual instance of the algorithm
    * @param algo
    *   the name of the algorithm (e.g. none, HS256 or HmacSHA256)
    */
  def optionFromString(algo: String): Option[JwtAlgorithm] =
    Option(algo).filterNot(_ == "none").map(fromString)

  def allHmac(): Seq[algorithms.JwtHmacAlgorithm] = Seq(HMD5, HS224, HS256, HS384, HS512)

  @nowarn("cat=deprecation")
  def allAsymmetric(): Seq[algorithms.JwtAsymmetricAlgorithm] =
    Seq(RS256, RS384, RS512, ES256, ES384, ES512, EdDSA, Ed25519)

  def allRSA(): Seq[algorithms.JwtRSAAlgorithm] = Seq(RS256, RS384, RS512)

  def allECDSA(): Seq[algorithms.JwtECDSAAlgorithm] = Seq(ES256, ES384, ES512)

  @nowarn("cat=deprecation")
  def allEdDSA(): Seq[algorithms.JwtEdDSAAlgorithm] = Seq(EdDSA, Ed25519)

  case object HMD5 extends algorithms.JwtHmacAlgorithm {
    def name = "HMD5"; def fullName = "HmacMD5"
  }
  case object HS224 extends algorithms.JwtHmacAlgorithm {
    def name = "HS224"; def fullName = "HmacSHA224"
  }
  case object HS256 extends algorithms.JwtHmacAlgorithm {
    def name = "HS256"; def fullName = "HmacSHA256"
  }
  case object HS384 extends algorithms.JwtHmacAlgorithm {
    def name = "HS384"; def fullName = "HmacSHA384"
  }
  case object HS512 extends algorithms.JwtHmacAlgorithm {
    def name = "HS512"; def fullName = "HmacSHA512"
  }
  case object RS256 extends algorithms.JwtRSAAlgorithm {
    def name = "RS256"; def fullName = "SHA256withRSA"
  }
  case object RS384 extends algorithms.JwtRSAAlgorithm {
    def name = "RS384"; def fullName = "SHA384withRSA"
  }
  case object RS512 extends algorithms.JwtRSAAlgorithm {
    def name = "RS512"; def fullName = "SHA512withRSA"
  }
  case object ES256 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES256"; def fullName = "SHA256withECDSA"
  }
  case object ES384 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES384"; def fullName = "SHA384withECDSA"
  }
  case object ES512 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES512"; def fullName = "SHA512withECDSA"
  }
  case object EdDSA extends algorithms.JwtEdDSAAlgorithm {
    def name = "EdDSA"; def fullName = "EdDSA"
  }
  @deprecated("Ed25519 is not a standard Json Web Algorithm name. Use EdDSA instead.", "9.3.0")
  case object Ed25519 extends algorithms.JwtEdDSAAlgorithm {
    def name = "Ed25519"; def fullName = "Ed25519"
  }
}
