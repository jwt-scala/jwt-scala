package pdi.jwt

trait JwtArrayByteLike[T] {
  def apply(key: T): Array[Byte]
  def unapply(key: Array[Byte]): T
}

object JwtArrayByteLike {
  def apply[T](key: T)(implicit env: JwtArrayByteLike[T]): Array[Byte] = env.apply(key)
  def unapply[T](key: Array[Byte])(implicit env: JwtArrayByteLike[T]): T = env.unapply(key)

  private val utf8Encoding = "UTF-8"
  private val iso8859Encoding = "ISO-8859-1"

  val utf8KeyToArrayByte: JwtArrayByteLike[String] = new JwtArrayByteLike[String] {
    override def apply(key: String): Array[Byte] = key.getBytes(utf8Encoding)
    override def unapply(key: Array[Byte]): String = new String(arr, utf8Encoding)
  }

  val iso8859KeyToArrayByte: JwtArrayByteLike[String] = new JwtArrayByteLike[String] {
    override def apply(key: String): Array[Byte] = key.getBytes(iso8859Encoding)
    override def unapply(key: Array[Byte]): String = new String(arr, iso8859Encoding)
  }

  val arrayByteKeyToArrayByte: JwtArrayByteLike[Array[Byte]] = new JwtArrayByteLike[Array[Byte]] {
    override def apply(key: Array[Byte]): Array[Byte] = key
    override def unapply(key: Array[Byte]): Array[Byte] = key
  }
}
