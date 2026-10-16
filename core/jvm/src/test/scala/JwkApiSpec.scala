package pdi.jwt

import java.math.BigInteger

class JwkApiSpec extends munit.FunSuite {
  import JwkTestKeys.*

  private def assertMetadata(
      key: Jwk,
      keyId: Option[String] = Some("original"),
      use: Option[JwkUse] = Some(JwkUse.Signature),
      algorithm: Option[String] = Some("original-alg"),
      keyOperations: Option[Set[String]] = Some(Set("verify"))
  ): Unit = {
    assertEquals(key.keyId, keyId)
    assertEquals(key.use, use)
    assertEquals(key.algorithm, algorithm)
    assertEquals(key.keyOperations, keyOperations)
  }

  private def metadataTests[A <: Jwk](name: String, create: () => A)(
      withKeyId: (A, Option[String]) => A,
      withUse: (A, Option[JwkUse]) => A,
      withAlgorithm: (A, Option[String]) => A,
      withKeyOperations: (A, Option[Set[String]]) => A
  ): Unit = {
    test(s"$name metadata updates should preserve the key material and other metadata") {
      val original = create()
      val changedId = withKeyId(original, Some("other"))
      val changedUse = withUse(original, Some(JwkUse.Encryption))
      val changedAlgorithm = withAlgorithm(original, Some("other-alg"))
      val changedOperations = withKeyOperations(original, Some(Set("sign")))
      val noId = withKeyId(original, None)
      val noUse = withUse(original, None)
      val noAlgorithm = withAlgorithm(original, None)
      val noOperations = withKeyOperations(original, None)

      assertMetadata(changedId, keyId = Some("other"))
      assertMetadata(changedUse, use = Some(JwkUse.Encryption))
      assertMetadata(changedAlgorithm, algorithm = Some("other-alg"))
      assertMetadata(changedOperations, keyOperations = Some(Set("sign")))
      assertMetadata(noId, keyId = None)
      assertMetadata(noUse, use = None)
      assertMetadata(noAlgorithm, algorithm = None)
      assertMetadata(noOperations, keyOperations = None)

      Seq(
        changedId,
        changedUse,
        changedAlgorithm,
        changedOperations,
        noId,
        noUse,
        noAlgorithm,
        noOperations
      ).foreach { updated =>
        assertNotEquals(updated, original)
        assertEquals(updated.keyMembers, original.keyMembers)
        assertEquals(updated.publicKey, original.publicKey)
        assertEquals(updated.thumbprint, original.thumbprint)
      }
      assertMetadata(original)
      assertEquals(original, create())
      assertEquals(original.hashCode(), create().hashCode())
      assertEquals(withKeyId(original, original.keyId), original)
      assertEquals(withUse(original, original.use), original)
      assertEquals(withAlgorithm(original, original.algorithm), original)
      assertEquals(withKeyOperations(original, original.keyOperations), original)
      assert(!original.equals(null))
      assert(!original.equals("not a key"))
    }
  }

  private def rsaKey(): RsaJwk = RsaJwk(
    Some("original"),
    Some(JwkUse.Signature),
    Some("original-alg"),
    new BigInteger(1, JwtBase64.decode(Rfc7638Modulus)),
    BigInteger.valueOf(65537),
    Some(Set("verify"))
  )

  metadataTests("RsaJwk", () => rsaKey())(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  private lazy val ecKey = ecPublic(ec(EcCurve.P256))

  metadataTests(
    "EcJwk",
    () =>
      EcJwk(
        Some("original"),
        Some(JwkUse.Signature),
        Some("original-alg"),
        EcCurve.P256,
        ecKey.getW.getAffineX,
        ecKey.getW.getAffineY,
        Some(Set("verify"))
      )
  )(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  metadataTests(
    "OkpJwk",
    () =>
      OkpJwk(
        Some("original"),
        Some(JwkUse.Signature),
        Some("original-alg"),
        OkpCurve.Ed25519,
        Rfc8037Ed25519X,
        Some(Set("verify"))
      )
  )(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  test("RsaJwk equality should include both key parameters") {
    val key = rsaKey()
    assertNotEquals(
      key,
      RsaJwk(
        key.keyId,
        key.use,
        key.algorithm,
        key.modulus.add(BigInteger.TWO),
        key.exponent,
        key.keyOperations
      )
    )
    assertNotEquals(
      key,
      RsaJwk(
        key.keyId,
        key.use,
        key.algorithm,
        key.modulus,
        BigInteger.valueOf(3),
        key.keyOperations
      )
    )
  }

  test("EcJwk equality should distinguish different public points") {
    val other = ecPublic(ec(EcCurve.P256))
    val first = EcJwk(None, None, None, EcCurve.P256, ecKey.getW.getAffineX, ecKey.getW.getAffineY)
    val second = EcJwk(None, None, None, EcCurve.P256, other.getW.getAffineX, other.getW.getAffineY)
    assertNotEquals(first, second)
  }

  test("JwkSet equality should include the ordered keys") {
    val first = rsaKey()
    val second = first.withKeyId(Some("other"))
    val set = JwkSet(Seq(first, second))
    val equal = JwkSet(List(rsaKey(), rsaKey().withKeyId(Some("other"))))
    assertEquals(set, equal)
    assertEquals(set.hashCode(), equal.hashCode())
    assertEquals(set.keys, Seq(first, second))
    assertNotEquals(set, JwkSet(Seq(second, first)))
    assertNotEquals(set, JwkSet(Seq(first)))
    assertNotEquals(set, JwkSet(Seq.empty))
    assert(!set.equals(null))
    assert(!set.equals(set.keys))
    assertEquals(set.toString, s"JwkSet(${set.keys})")
  }

  test("JwkUse.Other should retain value equality and its named accessor") {
    val first = JwkUse.Other("custom")
    val equal = JwkUse.Other("custom")
    assertEquals(first, equal)
    assertEquals(first.hashCode(), equal.hashCode())
    assertEquals(first.value, "custom")
    assertEquals(JwkUse.fromString("custom"), first)
    assertNotEquals(first, JwkUse.Other("other"))
    assert(!first.equals(null))
    assert(!first.equals(JwkUse.Signature))
    assert(!first.equals("custom"))
    assertEquals(first.toString, "Other(custom)")
  }

  test("key string representations should retain their type and public parameters") {
    val key = rsaKey()
    assertEquals(
      key.toString,
      s"RsaJwk(${key.keyId},${key.use},${key.algorithm},${key.modulus},${key.exponent},${key.keyOperations})"
    )
    val ecJwk = EcJwk(None, None, None, EcCurve.P256, ecKey.getW.getAffineX, ecKey.getW.getAffineY)
    assertEquals(
      ecJwk.toString,
      s"EcJwk(None,None,None,P256,${ecJwk.xCoordinate},${ecJwk.yCoordinate},None)"
    )
    val okpJwk = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)
    assertEquals(okpJwk.toString, s"OkpJwk(None,None,None,Ed25519,$Rfc8037Ed25519X,None)")
  }
}
