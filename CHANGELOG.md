# Changelog

## 2.0.0 (13/02/2019)

- Upgrade to Play 2.7.0 (thanks @prakhunov)
- Upgrade to play-json 2.7.0 (thanks @etspaceman)
- Drop support for Java 6 and 7

## 1.1.0 (09/01/2019)

- Upgrade to uPickle 0.7.1 (thanks @edombowsky)
- Add support for Argonaut (thanks @isbodand)

## 1.0.0 (25/11/2018)

- Bump bouncyCastle version to fix [CVE-2018-1000613](cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1000613) (thanks @djamelz)
- Also 1.0.0 for no reason except no feature was needed over the last months.

## 0.19.0 (20/10/2018)

**Breaking change**

This is actually a simple one but still... fixed a typo at `asymmetric` missing one `m`, just need to rename a few types to fix your code (thanks @DrPhil).

- Add support to `spray-json` (thanks @Slakah)
- Bump some versions (thanks @vhiairrassary)

## 0.18.0 (09/10/2018)

- Add support to `aud` being a simple string on uPickle (thanks @deterdw)
- Make all `parseHeader` and `parseClaim` methods public.

## 0.17.0 (29/07/2018)

- After consideration, release #84 , which mostly allow users to write custom parsers by extending jwt-scala ones. Doc page can be found here.

## 0.16.0 (05/03/2018)

- Adding Key ID property to JwtHeader as `kid` in JSON payload

## 0.15.0 (24/02/2018)

- Upgrade to uPickle 0.5.1
- Upgrade to Circe 0.9.1 (thanks @jan0sch)

## 0.14.1 (30/10/2017)

- Fix exception when `play.http.session.maxAge` is `null` in Play 2.6.x (thanks @austinpernell)

## 0.14.0 (07/07/2017)

- Add `play.http.session.jwtResponseName` to customize response header in Play (thanks @Isammoc)
- Fix code snippet style in docs

## 0.13.0 (08/06/2017)

- Upgrade to Circe 0.8.0 (thanks @dvic)
- Play 2.6 support (thanks @perotom)
- Bouncy Castle 1.57 (thanks @rwhitworth)

## 0.12.1 (29/03/2017)

- Support spaces in JSON for pure Scala JWT

## 0.12.0 (20/02/2017)

- **Breaking changes** I liked having all implicits directly inside the package object but it started to create problems. When generating the documentation, which depends on all projects, we had runtime errors while all tests were green, but they are ran on project at a time. Also, it means all implicits where always present on the scope which might not be the best option. So the idea is to move them from the package object to the `JwtXXX` object. For example, for Play Json:

```scala
// Before
// JwtJson.scala.
package pdi.jwt

object JwtJson extends JwtJsonCommon[JsObject] {
  // stuff...
}

// package.scala
package pdi

package object jwt extends JwtJsonImplicits {}

// --------------------------------------------------------
// After
// JwtJson.scala.
package pdi.jwt

object JwtJson extends JwtJsonCommon[JsObject] with JwtJsonImplicits {
  // stuff...
}
```

## 0.11.0 (19/02/2017)

- Drop Scala 2.10
- Play support is back

## 0.10.0 (02/02/2017)

- Support Scala 2.12.0
- Drop Play Framework support until it supports Scala 2.12
- Add uPickle support (thanks @alonsodomin)
- Update Play Json to 2.6.0-M1 for Scala 2.12 support
- Update Circe to 0.7.0

## 0.9.2 (10/11/2016)

- Support Circe 0.6.0 (thanks @TimothyKlim )

## 0.9.1 (10/11/2016)

- Support Json4s 3.5.0 (thanks @sanllanta)

## 0.9.0 (08/10/2016)

- Transformation of Signature to ASN.1 DER for ECDSA Algorithms (thanks @bestehle)
- Remove algorithm aliases to align with [JWA spec](https://tools.ietf.org/html/rfc7518#section-3.1)

## 0.8.1 (04/09/2016)

- Update to Circe 0.5.0

## 0.8.0 (05/07/2016)

- Update to Circe 0.4.1
- `audience` is now `Set[String]` rather than just `String` inside `Claim` according to JWT spec. API using `String` still available.
- Use `org.bouncycastle.util.Arrays.constantTimeAreEqual` to check signature rather than home made function.
- Remove Play Legacy since Play 2.5+ only supports Java 1.8+

## 0.7.1 (20/04/2016)

Add `leeway` support in `JwtOptions`

## 0.7.0 (17/03/2016)

Support for Circe 0.3.0

## 0.6.0 (09/03/2016)

Support for Play Framework 2.5.0

## 0.5.1 (05/03/2016)

Fix bug not-escaping quotation mark `"` when stringifying JSON.

## 0.5.0 (31/12/2015)

### Circe support

Thanks to @dwhitney , `JWT Scala` now has support for [Circe](https://github.com/travisbrown/circe). Check out [samples](http://pauldijou.fr/jwt-scala/samples/jwt-circe/) and [Scaladoc](http://pauldijou.fr/jwt-scala/api/latest/jwt-circe/).

### Disable validation

When decoding, `JWT Scala` also performs validation. If you need to decode an invalid token, you can now use a `JwtOptions` as the last argument of any decoding function to disable validation checks like expiration, notBefore and signature. Read the **Options** section of the [core sample](http://pauldijou.fr/jwt-scala/samples/jwt-core/) to know more.

### Fix null session in Play 2.4

Since 2.4, Play assign `null` as default value for some configuration keys which throw a `ConfigException.Null` in TypeSafe config lib. This should be fixed with the new configuration system at some point in the future. In the mean time, all calls reading the configuration will be wrapped in a try/catch to prevent that.

## 0.4.1 (30/09/2015)

Fix tricky bug inside all JSON libs not supporting correctly the `none` algorithm.

## 0.4.0 (24/07/2015)

Thanks a lot to @drbild for helping review the code around security vulnerabilities.

### Now on Maven

All the sub-projects are now released directly on Maven Central. Since Sonatype didn't accept `pdi` as the groupId, I had to change it to `com.pauldijou`. Sorry about that, you will need to quickly update your `build.sbt` (or whatever file contains your dependencies).

### Breaking changes

**Good news** Those changes don't impact the `jwt-play` lib, only low level APIs.

All decoding and validating methods with a `key: String` are now removed for security reasons. Please use their counterpart which now needs a 3rd argument corresponding to the list of algorithms that the token can be signed with. This list cannot mix HMAC and asymetric algorithms (like RSA or ECDSA). This is to prevent a server using RSA with a String key to receive a forged token signed with a HMAC algorithm and the RSA public key to be accepted using the same RSA public key as the HMAC secret key by default. You can learn more by reading [this article](https://www.timmclean.net/2015/03/31/jwt-algorithm-confusion.html).

```scala
// Before
val claim = Jwt.decode(token, key)

// After (knowing that you only expect a HMAC 256)
val claim = Jwt.decode(token, key, Seq(JwtAlgorithm.HS256))
// After (supporting all HMAC algorithms)
val claim = Jwt.decode(token, key, JwtAlgorithm.allHmac)
```

If you are using `SecretKey` or `PublicKey`, the list of algorithms is optional and will be automatically computed (using `JwtAlgorithm.allHmac` and `JwtAlgorithm.allAsymetric` respesctively) but feel free to provide you own list if you want to restrict the possible algorithms. More security never killed any web application.

Why not deprecate them? I considered doing that but I decided to enforce the security fix. I'm pretty sure that most people only use one HMAC algorithm with a String key and it will force them to edit their code but it should be a minor edit since you usually only decode tokens once or twice inside a code base. The fact that the project is still very new and at a `0.x` version played in the decision.

### Fixes

Fix a security vulnerability around timing attacks.

### Features

Add implicit class to convert `JwtHeader` and `JwtClaim` to `JsValue` or `JValue`. See [examples for Play JSON](http://pauldijou.fr/jwt-scala/samples/jwt-play-json/) or [examples for Json4s](http://pauldijou.fr/jwt-scala/samples/jwt-json4s/).

```scala
// Play JSON
JwtHeader(JwtAlgorithm.HS256).toJsValue
JwtClaim().by("me").to("you").about("something").issuedNow.startsNow.expiresIn(15).toJsValue

// Json4s
JwtHeader(JwtAlgorithm.HS256).toJValue
JwtClaim().by("me").to("you").about("something").issuedNow.startsNow.expiresIn(15).toJValue
```

## 0.2.1 (24/07/2015)

Same as `0.4.0` but targeting Play 2.3

## 0.3.0 (08/06/2015)

### Breaking changes

- move exceptions to their own package
- move algorithms to their own package

### Features

- support Play 2.4.0

## 0.2.0 (02/06/2015)

### Breaking changes

- removed all `Option` from API. Now, it's either nothing or a valid key. It shouldn't have a big impact since the majority of users were using valid keys already.
- when decoding a token to a `Tuple3`, the last part representing the signature is now a `String` rather than an `Option[String]`.

### New features

- full support for `SecretKey` for HMAC algorithms
- full support for `PrivateKey` and `PublicKey` for RSA and ECDSA algorithms
- Nearly all API now have 4 possible signatures (note: `JwtAsymetricAlgorithm` is either a RSA or a ECDSA algorithm)
  - `method(...)`
  - `method(..., key: String, algorithm: JwtAlgorithm)`
  - `method(..., key: SecretKey, algorithm: JwtHmacAlgorithm)`
  - `method(..., key: PrivateKey/PublicKey, algorithm: JwtAsymetricAlgorithm)`

Use `PrivateKey` when encoding and `PublicKey` when decoding or verifying.

### Bug fixes

- Some ECDSA algorithms were extending the wrong super-type
- `{"algo":"none"}` header was incorrectly supported

## 0.1.0 (18/05/2015)

No code change from 0.0.6, just more doc and tests.

## 0.0.6 (14/05/2015)

Add support for Json4s (both Native and Jackson implementations)

## 0.0.5 (13/05/2015)

We should be API ready. Just need more tests and scaladoc before production ready.
