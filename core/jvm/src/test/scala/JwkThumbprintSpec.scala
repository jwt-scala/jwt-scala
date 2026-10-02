package pdi.jwt

import java.math.BigInteger
import java.security.interfaces.RSAPublicKey
import java.security.{KeyPairGenerator, MessageDigest}

import pdi.jwt.exceptions.JwtLengthException

class JwkThumbprintSpec extends munit.FunSuite {
  private val rfc7638Modulus =
    "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPeb" +
      "WKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgn" +
      "Yb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44" +
      "-csFCur-kEgU8awapJzKnqDKgw"
  private val rfc7638Thumbprint = "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs"

  private def generate(): RSAPublicKey = {
    val generator = KeyPairGenerator.getInstance(JwtUtils.RSA)
    generator.initialize(2048)
    generator.generateKeyPair().getPublic.asInstanceOf[RSAPublicKey]
  }

  private def rfcKey(): RSAPublicKey = JwtUtils.rsaPublicKey(
    new BigInteger(1, JwtBase64.decode(rfc7638Modulus)),
    new BigInteger(1, JwtBase64.decode("AQAB"))
  )

  private def unsignedBytes(value: BigInteger): Array[Byte] = {
    val bytes = value.toByteArray
    if (bytes.length > 1 && bytes(0) == 0) bytes.drop(1) else bytes
  }

  private def independentThumbprint(modulus: BigInteger, exponent: BigInteger): String = {
    val encoder = java.util.Base64.getUrlEncoder.withoutPadding
    val json =
      "{\"e\":\"" + encoder.encodeToString(unsignedBytes(exponent)) +
        "\",\"kty\":\"RSA\",\"n\":\"" + encoder.encodeToString(unsignedBytes(modulus)) + "\"}"
    val digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes("UTF-8"))
    encoder.encodeToString(digest)
  }

  test("sha256 should match the RFC 7638 section 3.1 example") {
    assertEquals(JwkThumbprint.sha256(rfcKey()).base64Url, rfc7638Thumbprint)
  }

  test("sha256 should strip the sign byte of a modulus with its top bit set") {
    val modulus = BigInteger.ONE.shiftLeft(2047).add(BigInteger.ONE)
    val exponent = BigInteger.valueOf(65537)
    assertEquals(modulus.toByteArray.length, 257)
    assertEquals(modulus.toByteArray.apply(0).toInt, 0)
    val key = JwtUtils.rsaPublicKey(modulus, exponent)
    assertEquals(JwkThumbprint.sha256(key).base64Url, independentThumbprint(modulus, exponent))
  }

  test("sha256 should encode a small public exponent") {
    val modulus = BigInteger.ONE.shiftLeft(2047).add(BigInteger.ONE)
    val exponent = BigInteger.valueOf(3)
    val key = JwtUtils.rsaPublicKey(modulus, exponent)
    assertEquals(JwkThumbprint.sha256(key).base64Url, independentThumbprint(modulus, exponent))
  }

  test("sha256 should expose the 32 digest octets of the RFC 7638 section 3.1 example") {
    val bytes = JwkThumbprint.sha256(rfcKey()).toBytes
    assertEquals(bytes.length, 32)
    assertEquals(bytes.toList, JwtBase64.decode(rfc7638Thumbprint).toList)
    assertEquals(bytes(0) & 0xff, 55)
    assertEquals(bytes(31) & 0xff, 123)
  }

  test("uri should follow RFC 9278") {
    assertEquals(
      JwkThumbprint.sha256(rfcKey()).uri,
      "urn:ietf:params:oauth:jwk-thumbprint:sha-256:" + rfc7638Thumbprint
    )
  }

  test("toString should print the base64url form") {
    assertEquals(JwkThumbprint.sha256(rfcKey()).toString, rfc7638Thumbprint)
  }

  test("toBytes should return a defensive copy") {
    val thumbprint = JwkThumbprint.sha256(rfcKey())
    val first = thumbprint.toBytes
    first(0) = (first(0) ^ 0xff).toByte
    assertEquals(thumbprint.toBytes.toList, JwtBase64.decode(rfc7638Thumbprint).toList)
    assertEquals(thumbprint.base64Url, rfc7638Thumbprint)
    assert(!(thumbprint.toBytes eq thumbprint.toBytes))
  }

  test("equals and hashCode should be value based") {
    val a = JwkThumbprint.sha256(rfcKey())
    val b = JwkThumbprint.fromBase64Url(rfc7638Thumbprint)
    assertEquals(a, b)
    assertEquals(a.hashCode, b.hashCode)
    assertNotEquals(a, JwkThumbprint.sha256(generate()))
    assert(!a.equals(rfc7638Thumbprint))
  }

  test("the constructor should validate the digest it is given") {
    val tooShort = intercept[JwtLengthException](JwkThumbprint("AAAA"))
    assert(tooShort.getMessage.contains("32 bytes"), tooShort.getMessage)
    val notBase64Url = intercept[IllegalArgumentException](JwkThumbprint("not base64url!!"))
    assert(notBase64Url.getMessage.nonEmpty)
    assertEquals(JwkThumbprint(rfc7638Thumbprint).base64Url, rfc7638Thumbprint)
    assertEquals(JwkThumbprint(rfc7638Thumbprint), JwkThumbprint.sha256(rfcKey()))
  }

  test("fromBase64Url should give equal thumbprints for two spellings of the same digest") {
    val nonCanonical = rfc7638Thumbprint.dropRight(1) + "t"
    assertNotEquals(nonCanonical, rfc7638Thumbprint)
    assertEquals(JwtBase64.decode(nonCanonical).toList, JwtBase64.decode(rfc7638Thumbprint).toList)
    assertEquals(JwkThumbprint.fromBase64Url(nonCanonical).base64Url, rfc7638Thumbprint)
    assertEquals(
      JwkThumbprint.fromBase64Url(nonCanonical),
      JwkThumbprint.fromBase64Url(rfc7638Thumbprint)
    )
  }

  test("fromBase64Url should round trip") {
    val thumbprint = JwkThumbprint.sha256(generate())
    assertEquals(JwkThumbprint.fromBase64Url(thumbprint.base64Url), thumbprint)
  }

  test("fromBase64Url should reject an empty value") {
    intercept[JwtLengthException] {
      JwkThumbprint.fromBase64Url("")
    }
  }

  test("fromBase64Url should reject a value shorter than 32 bytes") {
    intercept[JwtLengthException] {
      JwkThumbprint.fromBase64Url("AAAA")
    }
  }

  test("fromBase64Url should reject a value longer than 32 bytes") {
    intercept[JwtLengthException] {
      JwkThumbprint.fromBase64Url(JwtBase64.encodeString(new Array[Byte](33)))
    }
  }

  test("fromBase64Url should reject non base64url input") {
    intercept[IllegalArgumentException] {
      JwkThumbprint.fromBase64Url("not base64url!!")
    }
  }

  test("fromBase64Url should reject standard base64 alphabet characters") {
    intercept[IllegalArgumentException] {
      JwkThumbprint.fromBase64Url(rfc7638Thumbprint.dropRight(1) + "+")
    }
  }

  test("sha256 should be stable for the same key") {
    val key = generate()
    val rebuilt = JwtUtils.rsaPublicKey(key.getModulus, key.getPublicExponent)
    assertEquals(JwkThumbprint.sha256(key), JwkThumbprint.sha256(key))
    assertEquals(JwkThumbprint.sha256(key), JwkThumbprint.sha256(rebuilt))
  }

  test("base64Url should be unpadded base64url of a 32 byte digest") {
    val thumbprint = JwkThumbprint.sha256(generate()).base64Url
    assertEquals(thumbprint.length, 43)
    assert(thumbprint.matches("[A-Za-z0-9_-]+"), thumbprint)
  }

  test("sha256 should differ between different keys") {
    assertNotEquals(JwkThumbprint.sha256(generate()), JwkThumbprint.sha256(generate()))
  }
}
