package pdi.jwt

object JwtBase64 extends JwtBase64Impl {
  // encode: Array[Byte] -> Array[Byte]
  // decode : Array[Byte] -> Array[Byte]

  def encode[T: JwtArrayByteLike](value: T): Array[Byte] = encode(JwtArrayByteLike.apply(value))
  // decode : String -> Array[Byte]

  // encodeString : Array[Byte] -> String
  def decodeString[T: JwtArrayByteLike](value: Array[Byte]): T = JwtArrayByteLike.unapply(decode(value))

  def encodeString[T: JwtArrayByteLike](value: T): String = encodeString(JwtArrayByteLike.apply(value))
  def decodeString[T: JwtArrayByteLike](value: T): String = decodeString(JwtArrayByteLike.apply(value))
}
