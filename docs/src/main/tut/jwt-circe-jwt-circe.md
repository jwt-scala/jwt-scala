## JwtCirce Object

### Basic usage

```tut
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

```tut
import java.time.Instant
import io.circe._, syntax._, jawn.{parse => jawnParse}
import cats.data.Xor
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val Xor.Right(claimJson) = jawnParse(s"""{"expires":${Instant.now.getEpochSecond}}""")
val Xor.Right(header) = jawnParse( """{"typ":"JWT","alg":"HS256"}""")
// From just the claim to all possible attributes
JwtCirce.encode(claimJson)
JwtCirce.encode(claimJson, key, algo)
JwtCirce.encode(header, claimJson, key)
```

### Decoding

```tut
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
