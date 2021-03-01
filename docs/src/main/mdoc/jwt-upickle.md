---
layout: docs
title:  "upickle"
position: 80
---

## JwtUpickle Object

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtUpickle$.html)

{% include_relative _install.md artifact="jwt-upickle" %}

### Basic usage

```scala mdoc:reset:silent
import java.time.Instant
import upickle.default._
import pdi.jwt.{JwtUpickle, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
  expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtUpickle.encode(claim, key, algo)

JwtUpickle.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc:reset:silent
import java.time.Instant
import upickle.default._
import pdi.jwt.{JwtUpickle, JwtAlgorithm, JwtClaim}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val claimJson = read[ujson.Value](s"""{"expires":${Instant.now.getEpochSecond}}""")
val header = read[ujson.Value]( """{"typ":"JWT","alg":"HS256"}""")
// From just the claim to all possible attributes
JwtUpickle.encode(claimJson)
JwtUpickle.encode(claimJson, key, algo)
JwtUpickle.encode(header, claimJson, key)
```

### Decoding

```scala mdoc:reset:silent
import java.time.Instant
import upickle.default._
import pdi.jwt.{JwtUpickle, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
  expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtUpickle.encode(claim, key, algo)

// You can decode to JsObject
JwtUpickle.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtUpickle.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtUpickle.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
