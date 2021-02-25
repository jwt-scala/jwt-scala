package pdi.jwt

import pdi.jwt.exceptions.JwtNonStringSetOrStringException
import upickle.default._

trait JwtUpickleImplicits {
  implicit val jwtUpickleHeaderReadWrite: ReadWriter[JwtHeader] =
    readwriter[ujson.Value].bimap[JwtHeader](
      header => ujson.read(header.toJson),
      json =>
        json match {
          case obj: ujson.Obj =>
            val fieldMap = obj.value.toMap
            JwtHeader(
              algorithm = fieldMap
                .get("alg")
                .map(_.str.toString())
                .flatMap(alg => JwtAlgorithm.optionFromString(alg)),
              typ = fieldMap.get("typ").map(_.str.toString()),
              contentType = fieldMap.get("cty").map(_.str.toString()),
              keyId = fieldMap.get("kid").map(_.str.toString())
            )
          case _ => throw new RuntimeException("Expected a ujson.Obj")
        }
    )

  implicit val jwtUpickleClaimReadWrite: ReadWriter[JwtClaim] =
    readwriter[ujson.Value].bimap[JwtClaim](
      claim => ujson.read(claim.toJson),
      json =>
        json match {
          case obj: ujson.Obj =>
            val fieldMap = obj.value.toMap
            val content = fieldMap -- Seq("iss", "sub", "aud", "exp", "nbf", "iat", "jti")
            JwtClaim(
              content = write(content),
              issuer = fieldMap.get("iss").map(_.str.toString()),
              subject = fieldMap.get("sub").map(_.str.toString()),
              audience = fieldMap.get("aud").map {
                case ujson.Arr(arr) => arr.map(_.str.toString()).toSet
                case ujson.Str(s)   => Set(s.toString)
                case _              => throw new JwtNonStringSetOrStringException("aud")
              },
              expiration = fieldMap.get("exp").map(_.num.toLong),
              notBefore = fieldMap.get("nbf").map(_.num.toLong),
              issuedAt = fieldMap.get("iat").map(_.num.toLong),
              jwtId = fieldMap.get("jti").map(_.str.toString())
            )
          case _ => throw new RuntimeException("Expected a ujson.Obj")
        }
    )
}
