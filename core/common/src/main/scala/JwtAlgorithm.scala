package pdi.jwt

sealed trait JwtAlgorithm {
  def name: String
}

object JwtAlgorithm {
  /**
    * @throws JwtNonSupportedAlgorithm in case the string doesn't match any known algorithm
    */
  def fromString(algo: String): JwtAlgorithm = algo match {
    case "HMD5"       => HmacMD5
    case "HS1"        => HmacSHA1
    case "HS224"      => HmacSHA224
    case "HS256"      => HmacSHA256
    case "HS384"      => HmacSHA384
    case "HS512"      => HmacSHA512
    case "HmacMD5"    => HmacMD5
    case "HmacSHA1"   => HmacSHA1
    case "HmacSHA224" => HmacSHA224
    case "HmacSHA256" => HmacSHA256
    case "HmacSHA384" => HmacSHA384
    case "HmacSHA512" => HmacSHA512
    case _            => throw new JwtNonSupportedAlgorithm(algo)
  }

  case object HmacMD5 extends JwtAlgorithm {
    def name = "HmacMD5"
  }

  case object HmacSHA1 extends JwtAlgorithm {
    def name = "HmacSHA1"
  }

  case object HmacSHA224 extends JwtAlgorithm {
    def name = "HmacSHA224"
  }

  case object HmacSHA256 extends JwtAlgorithm {
    def name = "HmacSHA256"
  }

  case object HmacSHA384 extends JwtAlgorithm {
    def name = "HmacSHA384"
  }

  case object HmacSHA512 extends JwtAlgorithm {
    def name = "HmacSHA512"
  }
}
