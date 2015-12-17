## JwtJson Object

### Basic usage

```tut

import java.time.Instant
import pdi.jwt.{JwtCirceJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond)
  , issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtCirceJson.encode(claim, key, algo)

JwtCirceJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtCirceJson.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```tut
import java.time.Instant
import io.circe._, syntax._, jawn.{parse => jawnParse}
import cats.data.Xor
import pdi.jwt.{JwtCirceJson, JwtAlgorithm, JwtClaim}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val Xor.Right(claimJson) = jawnParse(s"""{"expires":${Instant.now.getEpochSecond}}""")
val Xor.Right(header) = jawnParse( """{"typ":"JWT","alg":"HS256"}""")
// From just the claim to all possible attributes
JwtCirceJson.encode(claimJson)
JwtCirceJson.encode(claimJson, key, algo)
JwtCirceJson.encode(header, claimJson, key)
```

### Decoding

```tut
import java.time.Instant
import pdi.jwt.{JwtCirceJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond)
  , issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtCirceJson.encode(claim, key, algo)

// You can decode to JsObject
JwtCirceJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtCirceJson.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtCirceJson.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtCirceJson.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
