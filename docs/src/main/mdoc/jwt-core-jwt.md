---
layout: docs
title:  "Native"
position: 10
---

## Jwt object

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/Jwt$.html)

{% include_relative _install.md artifact="jwt-core" %}

### Basic usage

```scala mdoc:reset
import java.time.Clock
import pdi.jwt.{Jwt, JwtAlgorithm, JwtHeader, JwtClaim, JwtOptions}
implicit val clock: Clock = Clock.systemUTC
val token = Jwt.encode("""{"user":1}""", "secretKey", JwtAlgorithm.HS256)
Jwt.decodeRawAll(token, "secretKey", Seq(JwtAlgorithm.HS256))
Jwt.decodeRawAll(token, "wrongKey", Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc
// Encode from string, header automatically generated
Jwt.encode("""{"user":1}""", "secretKey", JwtAlgorithm.HS384)

// Encode from case class, header automatically generated
// Set that the token has been issued now and expires in 10 seconds
Jwt.encode(JwtClaim({"""{"user":1}"""}).issuedNow.expiresIn(10), "secretKey", JwtAlgorithm.HS512)

// You can encode without signing it
Jwt.encode("""{"user":1}""")

// You can specify a string header but also need to specify the algorithm just to be sure
// This is not really typesafe, so please use it with care
Jwt.encode("""{"typ":"JWT","alg":"HS256"}""", """{"user":1}""", "key", JwtAlgorithm.HS256)

// If using a case class header, no need to repeat the algorithm
// This is way better than the previous one
Jwt.encode(JwtHeader(JwtAlgorithm.HS256), JwtClaim("""{"user":1}"""), "key")
```

### Decoding

In JWT Scala, espcially when using raw strings which are not typesafe at all, there are a lot of possible errors. This is why nearly all `decode` functions will return a `Try` rather than directly the expected result. In case of failure, the wrapped exception should tell you what went wrong.

Take note that nearly all decoding methods (including those from helper libs) support either a String key, or a PrivateKey with a Hmac algorithm or a PublicKey with a RSA or ECDSA algorithm.

```scala mdoc
// Decode all parts of the token as string
Jwt.decodeRawAll(token, "secretKey", JwtAlgorithm.allHmac())

// Decode only the claim as a string
Jwt.decodeRaw(token, "secretKey", Seq(JwtAlgorithm.HS256))

// Decode all parts and cast them as a better type if possible.
// Since the implementation in JWT Core only use string, it is the same as decodeRawAll
// But check the result in JWT Play JSON to see the difference
Jwt.decodeAll(token, "secretKey", Seq(JwtAlgorithm.HS256))

// Same as before, but only the claim
// (you should start to see a pattern in the naming convention of the functions)
Jwt.decode(token, "secretKey", Seq(JwtAlgorithm.HS256))

// Failure because the token is not a token at all
Jwt.decode("Hey there!")

// Failure if not Base64 encoded
Jwt.decode("a.b.c")

// Failure in case we use the wrong key
Jwt.decode(token, "wrongKey", Seq(JwtAlgorithm.HS256))

// Failure if the token only starts in 5 seconds
Jwt.decode(Jwt.encode(JwtClaim().startsIn(5)))
```

### Validating

If you only want to check if a token is valid without decoding it. You have two options: `validate` functions that
return a `Try[Unit]` with the exceptions we saw in the decoding section, so you know what went wrong,
or `isValid` functions that will return a boolean in case you don't care about the actual error.

```scala mdoc
// All good
Jwt.validate(token, "secretKey", Seq(JwtAlgorithm.HS256))
Jwt.isValid(token, "secretKey", Seq(JwtAlgorithm.HS256))

// Wrong key here
Jwt.validate(token, "wrongKey", Seq(JwtAlgorithm.HS256))
Jwt.isValid(token, "wrongKey", Seq(JwtAlgorithm.HS256))

// No key for unsigned token => ok
Jwt.validate(Jwt.encode("{}"))
Jwt.isValid(Jwt.encode("{}"))

// No key while the token is actually signed => wrong
Jwt.validate(token)
Jwt.isValid(token)

// The token hasn't started yet!
Jwt.validate(Jwt.encode(JwtClaim().startsIn(5)))
Jwt.isValid(Jwt.encode(JwtClaim().startsIn(5)))

// This is no token
Jwt.validate("a.b.c")
Jwt.isValid("a.b.c")
```

### Options

All validating and decoding methods support a final optional argument as a `JwtOptions` which allow you to disable validation checks. This is useful if you need to access data from an expired token for example. You can disable `expiration`, `notBefore` and `signature` checks. Be warned that if you disable the last one, you have no guarantee that the user didn't change the content of the token.

```scala mdoc
val expiredToken = Jwt.encode(JwtClaim().by("me").expiresIn(-1))

// Fail since the token is expired
Jwt.isValid(expiredToken)
Jwt.decode(expiredToken)

// Let's disable expiration check
Jwt.isValid(expiredToken, JwtOptions(expiration = false))
Jwt.decode(expiredToken, JwtOptions(expiration = false))
```

You can also specify a leeway, in seconds, to account for clock skew.

```scala mdoc
// Allow 30sec leeway
Jwt.isValid(expiredToken, JwtOptions(leeway = 30))
Jwt.decode(expiredToken, JwtOptions(leeway = 30))
```
