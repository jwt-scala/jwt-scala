package pdi.jwt

import org.apache.commons.codec.binary.Base64

trait JwtBase64Impl {
  def encode(value: Array[Byte]): Array[Byte] = Base64.encodeBase64URLSafe(value)
  def decode(value: Array[Byte]): Array[Byte] = Base64.decodeBase64(value)

  def decode(value: String): Array[Byte] = Base64.decodeBase64(value)

  def encodeString(value: Array[Byte]): String = Base64.encodeBase64URLSafeString(value)

  def decodeNonSafe(value: Array[Byte]): Array[Byte] = Base64.decodeBase64(value)
  def decodeNonSafe(value: String): Array[Byte] = Base64.decodeBase64(value)
}
