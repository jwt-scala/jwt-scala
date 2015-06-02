## JwtJson Object

### Basic usage

```tut
import pdi.jwt.{JwtJson, JwtAlgorithm}, play.api.libs.json.Json
val claim = Json.obj(("user", 1), ("nbf", 1431520421))
val key = "secretKey"
val algo = JwtAlgorithm.HS256

JwtJson.encode(claim)

val token = JwtJson.encode(claim, key, algo)

JwtJson.decodeJson(token, key)

JwtJson.decode(token, key)
```

### Encoding

```tut
val header = Json.obj(("typ", "JWT"), ("alg", "HS256"))
// From just the claim to all possible attributes
JwtJson.encode(claim)
JwtJson.encode(claim, key, algo)
JwtJson.encode(header, claim, key)
```

### Decoding

```tut
// You can decode to JsObject
JwtJson.decodeJson(token, key)
JwtJson.decodeJsonAll(token, key)
// Or to case classes
JwtJson.decode(token, key)
JwtJson.decodeAll(token, key)
```

### Formating

The project provides implicit reader and writer for both `JwtHeader` and `JwtClaim`

```tut
import pdi.jwt._

// Reads
Json.fromJson[JwtHeader](header)
Json.fromJson[JwtClaim](claim)
// Writes
Json.toJson(JwtHeader(JwtAlgorithm.HS256))
Json.toJson(JwtClaim("""{"user":1}""").issuedNow.expiresIn(10))
```
