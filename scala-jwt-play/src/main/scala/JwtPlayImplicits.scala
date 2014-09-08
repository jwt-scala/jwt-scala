package pdi.scala.jwt

import play.api.Play
import play.api.mvc.{Result, RequestHeader}
import play.api.libs.json.JsObject

trait JwtPlayImplicits {
  implicit class RichResult(result: Result) {
    def jwtSession(implicit request: RequestHeader): JwtSession =
      result.header.headers.get(JwtSession.HEADER_NAME) match {
        case Some(header) => JwtSession.deserialize(header)
        case None => request.headers.get(JwtSession.HEADER_NAME).map(JwtSession.deserialize).getOrElse(new JwtSession())
      }

    def withJwtSession(session: JwtSession): Result = result.withHeaders(JwtSession.HEADER_NAME -> session.serialize)

    def withJwtSession(session: JsObject): Result = result.withJwtSession(new JwtSession(session))

    def withNewJwtSession: Result = result.withJwtSession(new JwtSession())

    /*def addingToSession(values: (String, String)*)(implicit request: RequestHeader): Result =
      withSession(new JwtSession(session.data ++ values.toMap))

    def removingFromSession(keys: String*)(implicit request: RequestHeader): Result =
      withSession(new JwtSession(session.data -- keys))*/
  }
}
