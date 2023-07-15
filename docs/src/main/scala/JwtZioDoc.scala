package pdi.jwt.docs

object JwtZioDoc {
  // #example
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtZIOJson}

  val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
  )
  val key = "secretKey"
  val algo = JwtAlgorithm.HS256

  val token = JwtZIOJson.encode(claim, key, algo)

  JwtZIOJson.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
  JwtZIOJson.decode(token, key, Seq(JwtAlgorithm.HS256))
  // #example
}

object ZioEncoding {
  // #encoding
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtZIOJson}
  import zio.json._
  import zio.json.ast._

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
  // #encoding
}

object ZioDecoding {
  // #decoding
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtZIOJson}

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
// #decoding
}
