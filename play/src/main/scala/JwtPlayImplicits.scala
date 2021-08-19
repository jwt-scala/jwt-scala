package pdi.jwt

import java.time.Clock
import javax.inject.Inject

import play.api.Configuration
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, JsString, Writes}
import play.api.mvc.{RequestHeader, Result}

trait JwtPlayImplicits {
  private def sanitizeHeader(header: String)(implicit conf: Configuration): String =
    if (header.startsWith(JwtSession.TOKEN_PREFIX)) {
      header.substring(JwtSession.TOKEN_PREFIX.length()).trim
    } else {
      header.trim
    }

  private def requestToJwtSession(
      request: RequestHeader
  )(implicit conf: Configuration, clock: Clock): JwtSession =
    request.headers
      .get(JwtSession.REQUEST_HEADER_NAME)
      .map(sanitizeHeader)
      .map(JwtSession.deserialize)
      .getOrElse(JwtSession())

  private def requestHasJwtHeader(request: RequestHeader)(implicit conf: Configuration): Boolean =
    request.headers.get(JwtSession.REQUEST_HEADER_NAME).isDefined

  /** By adding `import pdi.jwt._`, you will implicitely add all those methods to `Result` allowing
    * you to easily manipulate the [[JwtSession]] inside your Play application.
    *
    * {{{
    * package controllers
    *
    * import java.time.Clock
    * import play.api._
    * import play.api.mvc._
    * import pdi.jwt._
    *
    * object Application extends Controller {
    *   implicit val clock: Clock = Clock.systemUTC
    *
    *   def login = Action { implicit request =>
    *     Ok.addingToJwtSession(("logged", true))
    *   }
    *
    *   def logout = Action { implicit request =>
    *     Ok.withoutJwtSession
    *   }
    * }
    * }}}
    */
  implicit class RichResult @Inject() (result: Result)(implicit conf: Configuration, clock: Clock) {

    /** Check if the header for a [[JwtSession]] is present (first from the Result then from the
      * RequestHeader)
      * @return
      *   a Boolean indicating the presence of a JWT header
      */
    def hasJwtHeader(implicit request: RequestHeader): Boolean = {
      result.header.headers
        .get(JwtSession.RESPONSE_HEADER_NAME)
        .map(_ => true)
        .getOrElse(requestHasJwtHeader(request))
    }

    /** Retrieve the current [[JwtSession]] from the headers (first from the Result then from the
      * RequestHeader), if none, create a new one.
      * @return
      *   the JwtSession inside the headers or a new one
      */
    def jwtSession(implicit request: RequestHeader): JwtSession = {
      result.header.headers.get(JwtSession.RESPONSE_HEADER_NAME) match {
        case Some(token) => JwtSession.deserialize(sanitizeHeader(token))
        case None        => requestToJwtSession(request)
      }
    }

    /** If the Play app has a session.maxAge config, it will extend the expiration of the
      * [[JwtSession]] by that time, if not, it will do nothing.
      * @return
      *   the same Result with, eventually, a prolonged [[JwtSession]]
      */
    def refreshJwtSession(implicit request: RequestHeader): Result = JwtSession.MAX_AGE match {
      case None => result
      case _ => {
        // Only refresh if we actually have a proper JWT session
        if (hasJwtHeader) {
          result.withJwtSession(jwtSession.refresh())
        } else {
          result
        }
      }
    }

    /** Override the current [[JwtSession]] with a new one */
    def withJwtSession(session: JwtSession): Result = {
      result.withHeaders(
        JwtSession.RESPONSE_HEADER_NAME -> (JwtSession.TOKEN_PREFIX + session.serialize)
      )
    }

    /** Override the current [[JwtSession]] with a new one created from a JsObject */
    def withJwtSession(session: JsObject): Result = withJwtSession(JwtSession(session))

    /** Override the current [[JwtSession]] with a new one created from a sequence of tuples */
    def withJwtSession(fields: (String, JsValueWrapper)*): Result = withJwtSession(
      JwtSession(fields: _*)
    )

    /** Override the current [[JwtSession]] with a new empty one */
    def withNewJwtSession: Result = withJwtSession(JwtSession())

    /** Remove the current [[JwtSession]], which means removing the associated HTTP header */
    def withoutJwtSession: Result = result.copy(header =
      result.header.copy(headers = result.header.headers - JwtSession.RESPONSE_HEADER_NAME)
    )

    /** Keep the current [[JwtSession]] and add some values in it, if a key is already defined, it
      * will be overriden.
      */
    def addingToJwtSession(values: (String, String)*)(implicit request: RequestHeader): Result = {
      withJwtSession(jwtSession + new JsObject(values.map(kv => kv._1 -> JsString(kv._2)).toMap))
    }

    /** Keep the current [[JwtSession]] and add some values in it, if a key is already defined, it
      * will be overriden.
      */
    def addingToJwtSession[A: Writes](key: String, value: A)(implicit
        request: RequestHeader
    ): Result = {
      withJwtSession(jwtSession + (key, value))
    }

    /** Remove some keys from the current [[JwtSession]] */
    def removingFromJwtSession(keys: String*)(implicit request: RequestHeader): Result = {
      withJwtSession(jwtSession -- (keys: _*))
    }
  }

  /** By adding `import pdi.jwt._`, you will implicitely add this method to `RequestHeader` allowing
    * you to easily retrieve the [[JwtSession]] inside your Play application.
    *
    * {{{
    * package controllers
    *
    * import play.api._
    * import play.api.mvc._
    * import pdi.jwt._
    *
    * object Application extends Controller {
    *   def index = Action { request =>
    *     val session: JwtSession = request.jwtSession
    *   }
    * }
    * }}}
    */
  implicit class RichRequestHeader @Inject() (request: RequestHeader)(implicit
      conf: Configuration,
      clock: Clock
  ) {

    /** Return the current [[JwtSession]] from the request */
    def jwtSession: JwtSession = requestToJwtSession(request)

    /** Check if the request has the JWT header defined */
    def hasJwtHeader: Boolean = requestHasJwtHeader(request)
  }
}
