package pdi.jwt

object JwtUtils {
  val encoding = "UTF-8"

  def stringify(arr: Array[Byte]): String = new String(arr, encoding)
  def bytify(str: String): Array[Byte] = str.getBytes(encoding)

  def seqToJson(hash: Seq[(String, Any)]): String = hash.map {
    case (key, value: String) => "\"" + key + "\":\"" + value + "\""
    case (key, value: Boolean) => "\"" + key + "\":" + (if (value) { "true" } else { "false" })
    case (key, value: Double) => "\"" + key + "\":" + value.toString
    case (key, value: Short) => "\"" + key + "\":" + value.toString
    case (key, value: Float) => "\"" + key + "\":" + value.toString
    case (key, value: Long) => "\"" + key + "\":" + value.toString
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

  def sign(data: Array[Byte], key: Option[String], algorithm: Option[JwtAlgorithm]): Array[Byte] =
    (key, algorithm) match {
      case (Some(keyValue), Some(algo)) => {
        val mac = javax.crypto.Mac.getInstance(algo.fullName)
        mac.init(new javax.crypto.spec.SecretKeySpec(keyValue.getBytes(encoding), algo.fullName))
        mac.doFinal(data)
      }
      case _ => Array.empty[Byte]
    }

  def sign(data: String, key: Option[String], algorithm: Option[JwtAlgorithm]): Array[Byte] =
    sign(bytify(data), key, algorithm)
}
