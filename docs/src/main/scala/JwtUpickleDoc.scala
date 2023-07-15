package pdi.jwt.docs

object JwtUpickleDoc {
  // #example
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}

  val claim = JwtClaim(
    expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond)
  )
  val key = "secretKey"
  val algo = JwtAlgorithm.HS256

  val token = JwtUpickle.encode(claim, key, algo)

  JwtUpickle.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
  JwtUpickle.decode(token, key, Seq(JwtAlgorithm.HS256))
  // #example

  // #encoding
}

object UpickleEncode {
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtUpickle}
  import upickle.default._

  val key = "secretKey"
  val algo = JwtAlgorithm.HS256

  val claimJson = read[ujson.Value](s"""{"expires":${Instant.now.getEpochSecond}}""")
  val header = read[ujson.Value]("""{"typ":"JWT","alg":"HS256"}""")
  // From just the claim to all possible attributes
  JwtUpickle.encode(claimJson)
  JwtUpickle.encode(claimJson, key, algo)
  JwtUpickle.encode(header, claimJson, key)
  // #encoding
}

object UpickleDecode {
  // #decoding
  import java.time.Instant

  import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtUpickle}

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
  // #decoding
}
