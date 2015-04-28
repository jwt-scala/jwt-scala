package pdi.jwt

object JwtBase64 extends JwtBase64Impl {
  // encode: Array[Byte] -> Array[Byte]
  // decode : Array[Byte] -> Array[Byte]

  def encode(value: String): Array[Byte] = encode(JwtUtils.bytify(value))
  // decode : String -> Array[Byte]

  // encodeString : Array[Byte] -> String
  def decodeString(value: Array[Byte]): String = JwtUtils.stringify(decode(value))

  def encodeString(value: String): String = encodeString(JwtUtils.bytify(value))
  def decodeString(value: String): String = decodeString(JwtUtils.bytify(value))
}
