package pdi.jwt

import java.time.Clock
import scala.util.Try

import pdi.jwt.exceptions.{JwtNonStringException, JwtValidationException}
import upickle.default.*

/** Implementation of `JwtCore` using `Js.Value` from uPickle.
  *
  * To see a full list of samples, check the
  * [[https://jwt-scala.github.io/jwt-scala/jwt-upickle.html online documentation]].
  */
trait JwtUpickleParser[H, C] extends JwtJsonCommon[ujson.Value, H, C] with JwtUpickleImplicits {
  protected def parse(value: String): ujson.Value = ujson.read(value)

  protected def stringify(value: ujson.Value): String = ujson.write(value)

  protected def getAlgorithm(header: ujson.Value): Option[JwtAlgorithm] = header match {
    case obj: ujson.Obj =>
      val fields = obj.value.toMap
      fields.get("alg").flatMap(alg => JwtAlgorithm.optionFromString(alg.str.toString()))

    case _ => None
  }

  def parseJwk(json: String): Jwk = parse(json) match {
    case obj: ujson.Obj => readJwk(obj)
    case _              => throw new JwtValidationException("A JWK must be a JSON object.")
  }

  def parseJwkSet(json: String): JwkSet = {
    val keys = parse(json).objOpt
      .flatMap(_.get("keys"))
      .flatMap(_.arrOpt)
      .getOrElse(throw new JwtValidationException("A JWK Set must have a [keys] array."))
    JwkSet(
      keys.collect { case key: ujson.Obj => key }.flatMap(key => Try(readJwk(key)).toOption).toList
    )
  }

  private def readJwk(json: ujson.Obj): Jwk = Jwk.fromMembers(
    name =>
      json.value.get(name).map {
        case ujson.Str(value) => value
        case _                => throw new JwtNonStringException(name)
      },
    name =>
      json.value.get(name).map {
        case ujson.Arr(elements) =>
          val strings = elements.collect { case ujson.Str(value) => value }
          if (strings.length == elements.length) strings.toList
          else throw new JwtNonStringException(name)
        case _ => throw new JwtNonStringException(name)
      }
  )
}

object JwtUpickle extends JwtUpickle(Clock.systemUTC) {
  def apply(clock: Clock): JwtUpickle = new JwtUpickle(clock)
}

class JwtUpickle(override val clock: Clock) extends JwtUpickleParser[JwtHeader, JwtClaim] {
  def parseHeader(header: String): JwtHeader = read[JwtHeader](header)
  def parseClaim(claim: String): JwtClaim = read[JwtClaim](claim)
}
