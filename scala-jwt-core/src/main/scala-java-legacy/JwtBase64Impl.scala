package pdi.scala.jwt

import org.apache.commons.codec.binary.Base64

trait JwtBase64Impl {
  val codec = new Base64(true)

  def encode(value: Array[Byte]): Array[Byte] = codec.encode(value))
  def decode(value: Array[Byte]): Array[Byte] = codec.decode(value)
}
