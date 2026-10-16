package pdi.jwt

class JwkApiEvolutionSpec extends munit.FunSuite {
  import JwkTestKeys.*

  private lazy val rsaKey = Jwk.fromPublicKey(rsaPublic(rsa())).asInstanceOf[RsaJwk]
  private lazy val ecKey = Jwk.fromPublicKey(ecPublic(ec(EcCurve.P256))).asInstanceOf[EcJwk]
  private lazy val okpKey = OkpJwk(None, None, None, OkpCurve.Ed25519, Rfc8037Ed25519X)

  test("JWK values should not expose compiler-generated case-class APIs") {
    val values: Seq[AnyRef] = Seq(
      rsaKey,
      ecKey,
      okpKey,
      JwkSet(Seq(rsaKey)),
      rsaKey.thumbprint,
      JwkUse.Other("custom")
    )
    values.foreach { value =>
      assert(!value.isInstanceOf[Product], value.getClass.getName)
      assert(!value.getClass.getMethods.exists(_.getName == "copy"), value.getClass.getName)
    }

    val companions: Seq[AnyRef] = Seq(RsaJwk, EcJwk, OkpJwk, JwkSet, JwkThumbprint, JwkUse.Other)
    companions.foreach { companion =>
      assert(
        !companion.getClass.getMethods.exists(_.getName.startsWith("unapply")),
        companion.getClass.getName
      )
    }
  }

  private def metadataTests[A <: Jwk](name: String, original: () => A)(
      withKeyId: (A, Option[String]) => A,
      withUse: (A, Option[JwkUse]) => A,
      withAlgorithm: (A, Option[String]) => A,
      withKeyOperations: (A, Option[Set[String]]) => A
  ): Unit = {
    test(s"$name metadata updates should preserve value semantics and key material") {
      val key = original()
      val updated = withKeyOperations(
        withAlgorithm(withUse(withKeyId(key, Some("kid")), Some(JwkUse.Signature)), Some("alg")),
        Some(Set("verify"))
      )
      assertEquals(updated.keyId, Some("kid"))
      assertEquals(updated.use, Some(JwkUse.Signature))
      assertEquals(updated.algorithm, Some("alg"))
      assertEquals(updated.keyOperations, Some(Set("verify")))
      assertEquals(updated.keyMembers, key.keyMembers)
      assertEquals(updated.publicKey, key.publicKey)
      assertEquals(updated.thumbprint, key.thumbprint)
      assertEquals(key.keyId, None)
      assertEquals(key.use, None)
      assertEquals(key.algorithm, None)
      assertEquals(key.keyOperations, None)
      assertNotEquals(updated, key)
      val restored = withKeyOperations(
        withAlgorithm(withUse(withKeyId(updated, None), None), None),
        None
      )
      assertEquals(restored, key)
      assertEquals(restored.hashCode(), key.hashCode())
      assert(!key.equals(null))
    }
  }

  metadataTests("RsaJwk", () => rsaKey)(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  metadataTests("EcJwk", () => ecKey)(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  metadataTests("OkpJwk", () => okpKey)(
    (key, value) => key.withKeyId(value),
    (key, value) => key.withUse(value),
    (key, value) => key.withAlgorithm(value),
    (key, value) => key.withKeyOperations(value)
  )

  test("JwkSet should compare its ordered keys by value") {
    val set = JwkSet(Seq(rsaKey, ecKey))
    val equal = JwkSet(List(rsaKey.withKeyId(None), ecKey.withKeyId(None)))
    assertEquals(set, equal)
    assertEquals(set.hashCode(), equal.hashCode())
    assertNotEquals(set, JwkSet(Seq(ecKey, rsaKey)))
    assertNotEquals(set, JwkSet(Seq(rsaKey)))
    assert(!set.equals(null))
    assertEquals(set.toString, s"JwkSet(${set.keys})")
  }

  test("JwkUse.Other should compare its string value") {
    val use = JwkUse.Other("custom")
    assertEquals(use, JwkUse.Other("custom"))
    assertEquals(use.hashCode(), JwkUse.Other("custom").hashCode())
    assertEquals(use.value, "custom")
    assertEquals(JwkUse.fromString("custom"), use)
    assertNotEquals(use, JwkUse.Other("different"))
    assert(!use.equals(null))
    assert(!use.equals(JwkUse.Signature))
    assertEquals(use.toString, "Other(custom)")
  }
}
