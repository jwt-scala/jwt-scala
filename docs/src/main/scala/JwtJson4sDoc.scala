package pdi.jwt.docs

import scala.annotation.nowarn

@nowarn
object JwtJson4sDoc {
  // #example
  import org.json4s.JsonDSL.WithBigDecimal._
  import org.json4s._
  import pdi.jwt.{JwtAlgorithm, JwtJson4s}

  val claim = JObject(("user", 1), ("nbf", 1431520421))
  val key = "secretKey"
  val algo = JwtAlgorithm.HS256

  JwtJson4s.encode(claim)

  val token = JwtJson4s.encode(claim, key, algo)

  JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))

  JwtJson4s.decode(token, key, Seq(JwtAlgorithm.HS256))
  // #example

  // #encode
  val header = JObject(("typ", "JWT"), ("alg", "HS256"))

  JwtJson4s.encode(claim)
  JwtJson4s.encode(claim, key, algo)
  JwtJson4s.encode(header, claim, key)
  // #encode

  // #decode
  // You can decode to JsObject
  JwtJson4s.decodeJson(token, key, Seq(JwtAlgorithm.HS256))
  JwtJson4s.decodeJsonAll(token, key, Seq(JwtAlgorithm.HS256))
  // Or to case classes
  JwtJson4s.decode(token, key, Seq(JwtAlgorithm.HS256))
  JwtJson4s.decodeAll(token, key, Seq(JwtAlgorithm.HS256))
  // #decode
}
