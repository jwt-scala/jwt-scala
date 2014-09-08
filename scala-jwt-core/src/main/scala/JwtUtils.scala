package pdi.scala.jwt

object JwtUtils {
  val encoding = "UTF-8"

  def stringify(arr: Array[Byte]): String = new String(arr, encoding)
  def bytify(str: String): Array[Byte] = str.getBytes(encoding)

  def mapToJson(hash: Map[String, Any]): String = hash.map {
    case (key, value: String) => "\"" + key + "\":\"" + value + "\""
    case (key, value: Boolean) => "\"" + key + "\":" + (if (value) { "true" } else { "false" })
    case (key, value: Double) => "\"" + key + "\":" + value.toString
    case (key, value: Short) => "\"" + key + "\":" + value.toString
    case (key, value: Float) => "\"" + key + "\":" + value.toString
    case (key, value: Int) => "\"" + key + "\":" + value.toString
    case (key, value: Any) => "\"" + key + "\":\"" + value.toString + "\""
  }.mkString("{", ",", "}")

  def mergeJson(json: String, jsonSeq: String*): String = {
    val initJson = json.trim match {
      case "" => ""
      case value => value.drop(1).dropRight(1)
    }

    "{" + jsonSeq.map(_.trim).fold(initJson) {
      case (j1, result) if j1.length < 5 => result.drop(1).dropRight(1)
      case (result, j2) if j2.length < 7 => result
      case (j1, j2) => j1 + "," + j2.drop(1).dropRight(1)
    } + "}"
  }

  val algorithms = Seq("HmacMD5", "HmacSHA1", "HmacSHA224", "HmacSHA256", "HmacSHA384", "HmacSHA512")
  val aliases = Map(
    "HMD5"  -> "HmacMD5",
    "HS1"   -> "HmacSHA1",
    "HS224" -> "HmacSHA224",
    "HS256" -> "HmacSHA256",
    "HS384" -> "HmacSHA384",
    "HS512" -> "HmacSHA512"
  )

  private def getAlgo(algo: String): String = if (algorithms.contains(algo)) {
    algo
  } else {
    aliases.get(algo).getOrElse {
      throw new UnsupportedOperationException(algo + " is an unknown or unimplemented algorithm key. Possible values are [" + algorithms.mkString(", ") + ", " + aliases.keys.mkString(", ") + "]")
    }
  }

  def sign(data: Array[Byte], key: Option[String], algorithm: Option[String]): Array[Byte] = (key, algorithm) match {
    case (Some(keyValue), Some(algoValue)) => {
      val algo = getAlgo(algoValue)
      val mac = javax.crypto.Mac.getInstance(algo)
      mac.init(new javax.crypto.spec.SecretKeySpec(keyValue.getBytes(encoding), algo))
      mac.doFinal(data)
    }
    case _ => Array.empty[Byte]
  }

  def sign(data: String, key: Option[String], algorithm: Option[String]): Array[Byte] =
    sign(data.getBytes(encoding), key, algorithm)
}
