package pdi.jwt

trait JwtBase64Impl {
  private lazy val encoder = java.util.Base64.getUrlEncoder()
  private lazy val decoder = java.util.Base64.getUrlDecoder()

  def encode(value: Array[Byte]): Array[Byte] = encoder.encode(value)
  def decode(value: Array[Byte]): Array[Byte] = decoder.decode(value)
}
