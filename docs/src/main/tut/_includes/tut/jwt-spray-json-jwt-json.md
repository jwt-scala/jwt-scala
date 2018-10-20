## JwtSprayJson Object

### Basic usage

```tut
import java.time.Instant
import pdi.jwt.{JwtSprayJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtSprayJson.encode(claim, key, algo)

JwtSprayJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtSprayJson.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```tut
import java.time.Instant
import spray.json._
import pdi.jwt.{JwtSprayJson, JwtAlgorithm, JwtClaim}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val claimJson = s"""{"expires":${Instant.now.getEpochSecond}}""".parseJson.asJsObject
val header = """{"typ":"JWT","alg":"HS256"}""".parseJson.asJsObject
// From just the claim to all possible attributes
JwtSprayJson.encode(claimJson)
JwtSprayJson.encode(claimJson, key, algo)
JwtSprayJson.encode(header, claimJson, key)
```

### Decoding

```tut
import java.time.Instant
import pdi.jwt.{JwtSprayJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtSprayJson.encode(claim, key, algo)

// You can decode to JsObject
JwtSprayJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtSprayJson.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtSprayJson.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtSprayJson.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
