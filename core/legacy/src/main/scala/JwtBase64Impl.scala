package pdi.jwt

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils

trait JwtBase64Impl {
  // Apache Commons Codec fails grafully by returning empty string for anything it cannot decode
  // To simulate the new Java 1.8 behaviour, we need to throw IllegalArgumentException if the
  // value to decode is actually wrong
  private def validateBase64(value: Array[Byte]): Unit = if (value.length % 4 == 1) {
    if (value.length == 1) { throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes") }
    else { throw new IllegalArgumentException("Last unit does not have enough valid bits") }
  } else if (!Base64.isBase64(value)) {
    throw new IllegalArgumentException("Illegal base64 character")
  }
  
  private def validateBase64(value: String): Unit = validateBase64(StringUtils.getBytesUtf8(value))

  def encode(value: Array[Byte]): Array[Byte] = Base64.encodeBase64URLSafe(value)
  
  def decode(value: Array[Byte]): Array[Byte] = {
    validateBase64(value)
    Base64.decodeBase64(value)
  }

  def decode(value: String): Array[Byte] = {
    validateBase64(value)
    Base64.decodeBase64(value)
  }

  def encodeString(value: Array[Byte]): String = Base64.encodeBase64URLSafeString(value)

  def decodeNonSafe(value: Array[Byte]): Array[Byte] = {
    validateBase64(value)
    Base64.decodeBase64(value)
  }
  
  def decodeNonSafe(value: String): Array[Byte] = {
    validateBase64(value)
    Base64.decodeBase64(value)
  }
}
