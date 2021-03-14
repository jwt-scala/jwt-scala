---
layout: docs
title:  "Play Json"
position: 50
---

## JwtJson Object

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtJson$.html)

{% include_relative _install.md artifact="jwt-play-json" %}

### Basic usage

```scala mdoc
import java.time.Clock
import pdi.jwt.{JwtJson, JwtAlgorithm}
import play.api.libs.json.Json

implicit val clock: Clock = Clock.systemUTC

val claim = Json.obj(("user", 1), ("nbf", 1431520421))
val key = "secretKey"
val algo = JwtAlgorithm.HS256

JwtJson.encode(claim)

val token = JwtJson.encode(claim, key, algo)

JwtJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))

JwtJson.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc
val header = Json.obj(("typ", "JWT"), ("alg", "HS256"))
// From just the claim to all possible attributes
JwtJson.encode(claim)
JwtJson.encode(claim, key, algo)
JwtJson.encode(header, claim, key)
```

### Decoding

```scala mdoc
// You can decode to JsObject
JwtJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtJson.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtJson.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtJson.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```

### Formating

The project provides implicit reader and writer for both `JwtHeader` and `JwtClaim`

```scala mdoc
import pdi.jwt._
import pdi.jwt.JwtJson._

// Reads
Json.fromJson[JwtHeader](header)
Json.fromJson[JwtClaim](claim)

// Writes
Json.toJson(JwtHeader(JwtAlgorithm.HS256))
Json.toJson(JwtClaim("""{"user":1}""").issuedNow.expiresIn(10))
// Or
JwtHeader(JwtAlgorithm.HS256).toJsValue()
JwtClaim("""{"user":1}""").issuedNow.expiresIn(10).toJsValue()
```
