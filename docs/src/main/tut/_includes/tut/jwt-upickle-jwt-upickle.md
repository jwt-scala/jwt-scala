## JwtUpickle Object

**Compilation problem** Right now, even if all tests are green, there is a problem for compiling this documentation file.

### Basic usage

```
import java.time.Instant
import upickle.json
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

```
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val claimJson = json.read(s"""{"expires":${Instant.now.getEpochSecond}}""")
val header = json.read( """{"typ":"JWT","alg":"HS256"}""")
// From just the claim to all possible attributes
JwtUpickle.encode(claimJson)
JwtUpickle.encode(claimJson, key, algo)
JwtUpickle.encode(header, claimJson, key)
```

### Decoding

```
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
