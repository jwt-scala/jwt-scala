## JwtJson4s Object

### Basic usage

```tut
import pdi.jwt.{JwtJson4s, JwtAlgorithm}, org.json4s._, org.json4s.JsonDSL.WithBigDecimal._, org.json4s.native.JsonMethods._
val claim = JObject(("user", 1), ("nbf", 1431520421))
val key = "secretKey"
val algo = JwtAlgorithm.HS256

JwtJson4s.encode(claim)

val token = JwtJson4s.encode(claim, key, algo)

JwtJson4s.decodeJson(token, key)

JwtJson4s.decode(token, key)
```

### Encoding

```tut
val header = JObject(("typ", "JWT"), ("alg", "HS256"))
// From just the claim to all possible attributes
JwtJson4s.encode(claim)
JwtJson4s.encode(claim, None, None)
JwtJson4s.encode(claim, key, algo)
JwtJson4s.encode(claim, Option(key), Option(algo))
// This one will actually be unsigned since there is no key provided, even if there is an algorithm inside the header
JwtJson4s.encode(header, claim)
JwtJson4s.encode(header, claim, key)
JwtJson4s.encode(header, claim, Option(key))
```

### Decoding

```tut
// You can decode to JsObject
JwtJson4s.decodeJson(token, key)
JwtJson4s.decodeJson(token, Option(key))
JwtJson4s.decodeJsonAll(token, key)
JwtJson4s.decodeJsonAll(token, Option(key))
// Or to case classes
JwtJson4s.decode(token, key)
JwtJson4s.decode(token, Option(key))
JwtJson4s.decodeAll(token, key)
JwtJson4s.decodeAll(token, Option(key))
```
