---
layout: docs
title:  "Json4s"
position: 40
---

## JwtJson4s Object

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtJson4s$.html)

{% include_relative _install.md artifact="jwt-json4s-native" %}

### Basic usage

```scala mdoc
import pdi.jwt.{JwtJson4s, JwtAlgorithm}, org.json4s._, org.json4s.JsonDSL.WithBigDecimal._

val claim = JObject(("user", 1), ("nbf", 1431520421))
val key = "secretKey"
val algo = JwtAlgorithm.HS256

JwtJson4s.encode(claim)

val token = JwtJson4s.encode(claim, key, algo)

JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))

JwtJson4s.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc
val header = JObject(("typ", "JWT"), ("alg", "HS256"))

JwtJson4s.encode(claim)
JwtJson4s.encode(claim, key, algo)
JwtJson4s.encode(header, claim, key)
```

### Decoding

```scala mdoc
// You can decode to JsObject
JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtJson4s.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtJson4s.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtJson4s.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
