# Example: Play Framework + AngularJS

## Setup

~~~ shell
git clone git@github.com:pauldijou/jwt-scala.git
cd jwt-scala
sbt
project examplePlayAngularProject
run
~~~

Then go to [localhost:9000](http://localhost:9000) to see the small application running.

## Goal

The goal of the application is to demonstrate how to use JWT (Json Web Token) as a way to manage the user session rather than cookies, the default way in Play Framework. Using JWT, we take all the good parts (signature using `play.http.secret.key`), add a bit of JSON syntax, and put all that in an HTTP header. So it's just as secured as a cookie would be, it's just at a different place.

One nice benefit of JWT is that... it is not cookies! Cookies can be disabled (sometime by default... I'm looking at you Windows Phone), people don't like them (I'm talking about the ones in the internet). One major problem is that... it is not cookies. Those are automatically send with every request. With JWT, we need to manually add the header each time we send a request. You will also need to persist them client-side whereas the browser would have done that for you using cookies. There is no silver bullet for that, it depends mostly on what technologies you are using for your front-end, but you can use [this micro-library](https://github.com/pauldijou/jwt-client) that I quickly wrote to help you reading and storing the token. In the case of this application, we will take advantage of the HTTP interceptors provided by AngularJS to achieve the final result.  

## API

TODO: generate ScalaDoc

The API is nearly the same as the Session one inside Play, just renamed JwtSession. For example, the method `Result.withSession(session: Session)` is now `Result.withJwtSession(session: JwtSession)`. The other main different is that the session is no longer a `Map[String, String]` but rather a `JsObject` (actually two because there are the header part and the claim part, but you should mostly be interested in the claim part).

After that, we just use Action composition inside Play to handle all our security in one place. You can read the [Secured trait](https://github.com/pauldijou/jwt-scala/blob/master/examples/play-angular/app/controllers/Secured.scala) that group everything.

## How to...

### Write in the session

~~~ scala
Ok.addingToJwtSession("user", User("Paul"))
~~~

You can directly write case classes inside the session as long as you have an implicit `Writes` somewhere around. Which is as easy as...

~~~ scala
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class User(name: String)
object User {
  implicit val userFormat = Json.format[User]
}
~~~

### Read from the session

~~~ scala
request.jwtSession.getAs[User]("user")
// Return Option[User]
~~~

Just as before, you can directly read case classes from session if there is an implicit `Reads` around. Which is as easy as... oh wait, we have already done that!

## More stuff...

- There is a fully commented [application.conf](https://github.com/pauldijou/jwt-scala/blob/master/examples/play-angular/conf/application.conf) file so you can see the few new configuration keys you can use.
- You can read the [commented JavaScript file](https://github.com/pauldijou/jwt-scala/blob/master/examples/play-angular/public/javascripts/app.js) that handle all the logic for client-side.
