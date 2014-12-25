package pdi.scala.jwt

import play.api.Play
import play.api.mvc.{Result, RequestHeader}
import play.api.libs.json.{Json, JsObject, JsString, Writes}
import play.api.libs.json.Json.JsValueWrapper

trait JwtPlayImplicits {
  private def sanitizeHeader(header: String): String =
    if (header.startsWith(JwtSession.TOKEN_PREFIX)) {
      header.substring(JwtSession.TOKEN_PREFIX.length()).trim
    } else {
      header.trim
    }

  private def requestToJwtSession(request: RequestHeader): JwtSession =
    request.headers.get(JwtSession.HEADER_NAME).map(sanitizeHeader).map(JwtSession.deserialize).getOrElse(JwtSession())

  implicit class RichResult(result: Result) {
    def jwtSession(implicit request: RequestHeader): JwtSession =
      result.header.headers.get(JwtSession.HEADER_NAME) match {
        case Some(token) => JwtSession.deserialize(sanitizeHeader(token))
        case None => requestToJwtSession(request)
      }

    def refreshJwtSession(implicit request: RequestHeader): Result = JwtSession.MAX_AGE match {
      case None => result
      case _ => result.withJwtSession(jwtSession.refresh)
    }

    def withJwtSession(session: JwtSession): Result =
      result.withHeaders(JwtSession.HEADER_NAME -> (JwtSession.TOKEN_PREFIX + session.serialize))

    def withJwtSession(session: JsObject): Result = result.withJwtSession(JwtSession(session))

    def withJwtSession(fields: (String, JsValueWrapper)*): Result = result.withJwtSession(JwtSession(fields: _*))

    def withNewJwtSession: Result = result.withJwtSession(JwtSession())

    def addingToJwtSession(values: (String, String)*)(implicit request: RequestHeader): Result =
      withJwtSession(jwtSession + new JsObject(values.map(kv => kv._1 -> JsString(kv._2))))

    def addingToJwtSession[A: Writes](key: String, value: A)(implicit request: RequestHeader): Result =
      withJwtSession(jwtSession + (key, value))

    def removingFromJwtSession(keys: String*)(implicit request: RequestHeader): Result =
      withJwtSession(jwtSession -- (keys: _*))
  }

  implicit class RichRequestHeader(request: RequestHeader) {
    def jwtSession: JwtSession = requestToJwtSession(request)
  }
}
