---
layout: docs
title:  "Circe"
position: 30
---

## JwtCirce Object

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtCirce$.html)

{% include_relative _install.md artifact="jwt-circe" %}

### Basic usage

```scala mdoc:reset
import java.time.Instant
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond)
  , issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtCirce.encode(claim, key, algo)

JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtCirce.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc:reset
import java.time.Instant
import io.circe._, jawn.{parse => jawnParse}
import pdi.jwt.{JwtCirce, JwtAlgorithm}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val Right(claimJson) = jawnParse(s"""{"expires":${Instant.now.getEpochSecond}}""")
val Right(header) = jawnParse( """{"typ":"JWT","alg":"HS256"}""")
// From just the claim to all possible attributes
JwtCirce.encode(claimJson)
JwtCirce.encode(claimJson, key, algo)
JwtCirce.encode(header, claimJson, key)
```

### Decoding

```scala mdoc:reset
import java.time.Instant
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond)
  , issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtCirce.encode(claim, key, algo)

// You can decode to JsObject
JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtCirce.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtCirce.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtCirce.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
