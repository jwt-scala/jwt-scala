package pdi.jwt

sealed trait JwtAlgorithm {
  def name: String
  def fullName: String
}

object JwtAlgorithm {
  /**
    * @throws JwtNonSupportedAlgorithm in case the string doesn't match any known algorithm
    */
  def fromString(algo: String): JwtAlgorithm = algo match {
    case "HMD5"       => HMD5
    case "HS1"        => HS1
    case "HS224"      => HS224
    case "HS256"      => HS256
    case "HS384"      => HS384
    case "HS512"      => HS512
    case "HmacMD5"    => HmacMD5
    case "HmacSHA1"   => HmacSHA1
    case "HmacSHA224" => HmacSHA224
    case "HmacSHA256" => HmacSHA256
    case "HmacSHA384" => HmacSHA384
    case "HmacSHA512" => HmacSHA512
    case _            => throw new JwtNonSupportedAlgorithm(algo)
  }

  case object HMD5 extends JwtAlgorithm {
    def name = "HMD5"
    def fullName = "HmacMD5"
  }

  case object HmacMD5 extends JwtAlgorithm {
    def name = "HmacMD5"
    def fullName = "HmacMD5"
  }

  case object HS1 extends JwtAlgorithm {
    def name = "HS1"
    def fullName = "HmacSHA1"
  }

  case object HmacSHA1 extends JwtAlgorithm {
    def name = "HmacSHA1"
    def fullName = "HmacSHA1"
  }

  case object HS224 extends JwtAlgorithm {
    def name = "HS224"
    def fullName = "HmacSHA224"
  }

  case object HmacSHA224 extends JwtAlgorithm {
    def name = "HmacSHA224"
    def fullName = "HmacSHA224"
  }

  case object HS256 extends JwtAlgorithm {
    def name = "HS256"
    def fullName = "HmacSHA256"
  }

  case object HmacSHA256 extends JwtAlgorithm {
    def name = "HmacSHA256"
    def fullName = "HmacSHA256"
  }

  case object HS384 extends JwtAlgorithm {
    def name = "HS384"
    def fullName = "HmacSHA384"
  }

  case object HmacSHA384 extends JwtAlgorithm {
    def name = "HmacSHA384"
    def fullName = "HmacSHA384"
  }

  case object HS512 extends JwtAlgorithm {
    def name = "HS512"
    def fullName = "HmacSHA512"
  }

  case object HmacSHA512 extends JwtAlgorithm {
    def name = "HmacSHA512"
    def fullName = "HmacSHA512"
  }
}
