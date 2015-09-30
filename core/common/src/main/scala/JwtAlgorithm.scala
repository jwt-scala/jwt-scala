package pdi.jwt

import pdi.jwt.exceptions.JwtNonSupportedAlgorithm

sealed trait JwtAlgorithm {
  def name: String
  def fullName: String
}

package algorithms {
  sealed trait JwtAsymetricAlgorithm extends JwtAlgorithm {}

  sealed trait JwtHmacAlgorithm extends JwtAlgorithm {}
  sealed trait JwtRSAAlgorithm extends JwtAsymetricAlgorithm {}
  sealed trait JwtECDSAAlgorithm extends JwtAsymetricAlgorithm {}
}

object JwtAlgorithm {
  /** Deserialize an algorithm from its string equivalent. Only real algorithms supported,
    * if you need to support "none", use "optionFromString".
    *
    * @return the actual instance of the algorithm
    * @param algo the name of the algorithm (e.g. HS256 or HmacSHA256)
    * @throws JwtNonSupportedAlgorithm in case the string doesn't match any known algorithm
    */
  def fromString(algo: String): JwtAlgorithm = algo match {
    case "HMD5"        => HMD5
    case "HS1"         => HS1
    case "HS224"       => HS224
    case "HS256"       => HS256
    case "HS384"       => HS384
    case "HS512"       => HS512
    case "HmacMD5"     => HmacMD5
    case "HmacSHA1"    => HmacSHA1
    case "HmacSHA224"  => HmacSHA224
    case "HmacSHA256"  => HmacSHA256
    case "HmacSHA384"  => HmacSHA384
    case "HmacSHA512"  => HmacSHA512
    case "RS1"         => RS1
    case "RS256"       => RS256
    case "RS384"       => RS384
    case "RS512"       => RS512
    case "RSASHA1"     => RSASHA1
    case "RSASHA256"   => RSASHA256
    case "RSASHA384"   => RSASHA384
    case "RSASHA512"   => RSASHA512
    case "ES1"         => ES1
    case "ES256"       => ES256
    case "ES384"       => ES384
    case "ES512"       => ES512
    case "ECDSASHA1"   => ECDSASHA1
    case "ECDSASHA256" => ECDSASHA256
    case "ECDSASHA384" => ECDSASHA384
    case "ECDSASHA512" => ECDSASHA512
    case _             => throw new JwtNonSupportedAlgorithm(algo)
  }

  /** Deserialize an algorithm from its string equivalent. If it's the special "none" algorithm,
    * return None, else, return Some with the corresponding algorithm inside.
    *
    * @return the actual instance of the algorithm
    * @param algo the name of the algorithm (e.g. none, HS256 or HmacSHA256)
    * @throws JwtNonSupportedAlgorithm in case the string doesn't match any known algorithm nor "none"
    */
  def optionFromString(algo: String): Option[JwtAlgorithm] = if (algo == "none") {
    None
  } else {
    Some(fromString(algo))
  }

  def allHmac(): Seq[algorithms.JwtHmacAlgorithm] = Seq(HMD5, HS1, HS224, HS256, HS384, HS512, HmacMD5, HmacSHA1, HmacSHA224, HmacSHA256, HmacSHA384, HmacSHA512)

  def allAsymetric(): Seq[algorithms.JwtAsymetricAlgorithm] = Seq(RS1, RS256, RS384, RS512, RSASHA1, RSASHA256, RSASHA384, RSASHA512, ES1, ES256, ES384, ES512, ECDSASHA1, ECDSASHA256, ECDSASHA384, ECDSASHA512)

  def allRSA(): Seq[algorithms.JwtRSAAlgorithm] = Seq(RS1, RS256, RS384, RS512, RSASHA1, RSASHA256, RSASHA384, RSASHA512)

  def allECDSA(): Seq[algorithms.JwtECDSAAlgorithm] = Seq(ES1, ES256, ES384, ES512, ECDSASHA1, ECDSASHA256, ECDSASHA384, ECDSASHA512)

  case object HMD5 extends algorithms.JwtHmacAlgorithm {
    def name = "HMD5"
    def fullName = "HmacMD5"
  }

  case object HmacMD5 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacMD5"
    def fullName = "HmacMD5"
  }

  case object HS1 extends algorithms.JwtHmacAlgorithm {
    def name = "HS1"
    def fullName = "HmacSHA1"
  }

  case object HmacSHA1 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacSHA1"
    def fullName = "HmacSHA1"
  }

  case object HS224 extends algorithms.JwtHmacAlgorithm {
    def name = "HS224"
    def fullName = "HmacSHA224"
  }

  case object HmacSHA224 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacSHA224"
    def fullName = "HmacSHA224"
  }

  case object HS256 extends algorithms.JwtHmacAlgorithm {
    def name = "HS256"
    def fullName = "HmacSHA256"
  }

  case object HmacSHA256 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacSHA256"
    def fullName = "HmacSHA256"
  }

  case object HS384 extends algorithms.JwtHmacAlgorithm {
    def name = "HS384"
    def fullName = "HmacSHA384"
  }

  case object HmacSHA384 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacSHA384"
    def fullName = "HmacSHA384"
  }

  case object HS512 extends algorithms.JwtHmacAlgorithm {
    def name = "HS512"
    def fullName = "HmacSHA512"
  }

  case object HmacSHA512 extends algorithms.JwtHmacAlgorithm {
    def name = "HmacSHA512"
    def fullName = "HmacSHA512"
  }

  case object RS1 extends algorithms.JwtRSAAlgorithm {
    def name = "RS1"
    def fullName = "SHA1withRSA"
  }

  case object RSASHA1 extends algorithms.JwtRSAAlgorithm {
    def name = "RSASHA1"
    def fullName = "SHA1withRSA"
  }

  case object RS256 extends algorithms.JwtRSAAlgorithm {
    def name = "RS256"
    def fullName = "SHA256withRSA"
  }

  case object RSASHA256 extends algorithms.JwtRSAAlgorithm {
    def name = "RSASHA256"
    def fullName = "SHA256withRSA"
  }

  case object RS384 extends algorithms.JwtRSAAlgorithm {
    def name = "RS384"
    def fullName = "SHA384withRSA"
  }

  case object RSASHA384 extends algorithms.JwtRSAAlgorithm {
    def name = "RSASHA384"
    def fullName = "SHA384withRSA"
  }

  case object RS512 extends algorithms.JwtRSAAlgorithm {
    def name = "RS512"
    def fullName = "SHA512withRSA"
  }

  case object RSASHA512 extends algorithms.JwtRSAAlgorithm {
    def name = "RSASHA512"
    def fullName = "SHA512withRSA"
  }

  case object ES1 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES1"
    def fullName = "SHA1withECDSA"
  }

  case object ECDSASHA1 extends algorithms.JwtECDSAAlgorithm {
    def name = "ECDSASHA1"
    def fullName = "SHA1withECDSA"
  }

  case object ES256 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES256"
    def fullName = "SHA256withECDSA"
  }

  case object ECDSASHA256 extends algorithms.JwtECDSAAlgorithm {
    def name = "ECDSASHA256"
    def fullName = "SHA256withECDSA"
  }

  case object ES384 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES384"
    def fullName = "SHA384withECDSA"
  }

  case object ECDSASHA384 extends algorithms.JwtECDSAAlgorithm {
    def name = "ECDSASHA384"
    def fullName = "SHA384withECDSA"
  }

  case object ES512 extends algorithms.JwtECDSAAlgorithm {
    def name = "ES512"
    def fullName = "SHA512withECDSA"
  }

  case object ECDSASHA512 extends algorithms.JwtECDSAAlgorithm {
    def name = "ECDSASHA512"
    def fullName = "SHA512withECDSA"
  }
}
