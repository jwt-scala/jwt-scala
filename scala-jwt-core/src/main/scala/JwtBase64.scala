package pdi.jwt

object JwtBase64 extends JwtBase64Impl {
  def encode(value: String): Array[Byte] = encode(JwtUtils.bytify(value))
  def decode(value: String): Array[Byte] = decode(JwtUtils.bytify(value))

  def encodeString(value: Array[Byte]): String = JwtUtils.stringify(encode(value))
  def decodeString(value: Array[Byte]): String = JwtUtils.stringify(decode(value))

  def encodeString(value: String): String = JwtUtils.stringify(encode(value))
  def decodeString(value: String): String = JwtUtils.stringify(decode(value))
}
