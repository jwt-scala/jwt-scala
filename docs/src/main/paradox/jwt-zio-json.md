## ZIO Json

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtZioJson$.html)

@@@vars

```scala
libraryDependencies += "com.github.jwt-scala" %% "jwt-zio-json" % "$project.version$"
```

@@@

### Basic usage

```scala mdoc:reset
import java.time.Instant
import pdi.jwt.{JwtZIOJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
  expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtZIOJson.encode(claim, key, algo)

JwtZIOJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtZIOJson.decode(token, key, Seq(JwtAlgorithm.HS256))
```

### Encoding

```scala mdoc:reset
import java.time.Instant
import zio.json._
import zio.json.ast._
import pdi.jwt.{JwtZIOJson, JwtAlgorithm}

val key = "secretKey"
val algo = JwtAlgorithm.HS256

val claimJsonEither = s"""{"expires":${Instant.now.getEpochSecond}}""".fromJson[Json]
val headerEither = """{"typ":"JWT","alg":"HS256"}""".fromJson[Json]
// From just the claim to all possible attributes
for {
  claimJson <- claimJsonEither
  header <- headerEither
} yield {
  JwtZIOJson.encode(claimJson)
  JwtZIOJson.encode(claimJson, key, algo)
  JwtZIOJson.encode(header, claimJson, key)
}
```

### Decoding

```scala mdoc:reset
import java.time.Instant
import pdi.jwt.{JwtZIOJson, JwtAlgorithm, JwtClaim}

val claim = JwtClaim(
  expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
  issuedAt = Some(Instant.now.getEpochSecond)
)
val key = "secretKey"
val algo = JwtAlgorithm.HS256

val token = JwtZIOJson.encode(claim, key, algo)

// You can decode to JsObject
JwtZIOJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
JwtZIOJson.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
// Or to case classes
JwtZIOJson.decode(token, key, Seq(JwtAlgorithm.HS256))
JwtZIOJson.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
```
