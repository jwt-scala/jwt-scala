## JwtArgonaut object

### Basic usage

```tut
import java.time.Instant
import scala.util.Try

import pdi.jwt.{JwtAlgorithm, JwtArgonaut, JwtClaim}

import argonaut.Json

val claim = JwtClaim(
  expiration = Some(Instant.now().plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)

val key = "secretKey"
val alg = JwtAlgorithm.HS512

val token = JwtArgonaut.encode(claim, key, alg)
val decodedJson: Try[Json] = JwtArgonaut.decodeJson(token, key, Seq(alg))
val decodedClaim: Try[JwtClaim] = JwtArgonaut.decode(token, key, Seq(alg))
```

### Encoding

```tut
import argonaut.Parse
import pdi.jwt.{JwtAlgorithm, JwtArgonaut}

val key = "secretKey"
val alg = JwtAlgorithm.HS512

val jsonClaim = Parse.parseOption(s"""{"expires":${Instant.now().getEpochSecond}}""").get
val jsonHeader = Parse.parseOption("""{"typ":"JWT","alg":"HS512"}""").get

val token1: String = JwtArgonaut.encode(jsonClaim)
val token2: String = JwtArgonaut.encode(jsonClaim, key, alg)
val token3: String = JwtArgonaut.encode(jsonHeader, jsonClaim, key)
```

### Decoding

```tut
import scala.util.Try

import argonaut.Json
import pdi.jwt.{JwtAlgorithm, JwtArgonaut, JwtClaim, JwtHeader}

val claim = JwtClaim(
  expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val alg = JwtAlgorithm.HS512

val token = JwtArgonaut.encode(claim, key, alg)

val decodedJsonClaim: Try[Json] = JwtArgonaut.decodeJson(token, key, Seq(alg))
val decodedJson: Try[(Json, Json, String)] = JwtArgonaut.decodeJsonAll(token, key, Seq(alg))

val decodedClaim: Try[JwtClaim]  = JwtArgonaut.decode(token, key, Seq(alg))
val decodedToken: Try[(JwtHeader, JwtClaim, String)] = JwtArgonaut.decodeAll(token, key, Seq(alg))
```
