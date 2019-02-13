## JwtSession case class

Provides an API similar to the Play [Session](https://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.mvc.Session) but using `JsValue` rather than `String` as values. It also separates `headerData` from `claimData` rather than having only one `data`.

### Basic usage

```tut
import pdi.jwt.JwtSession
import play.api.Configuration


//In a real Play! App this should normally be injected in the constructor with @Inject()
implicit val conf:Configuration = Configuration.reference

// Let's create a session, it will automatically assign a default header. No
// In your app, the default header would be generated from "application.conf" file
// but here, it will just use the default values (which are all empty)
var session = JwtSession()

// We can add a (key, value)
session = session + ("user", 1)

// Or several of them
session = session ++ (("nbf", 1431520421), ("key", "value"), ("key2", 2), ("key3", 3))

// Also remove a key
session = session - "key"

// Or several
session = session -- ("key2", "key3")

// We can access a specific key
session.get("user")

// Test if the session is empty or not
// (it is not here since we have several keys in the claimData)
session.isEmpty

// Serializing the session is the same as encoding it as a JSON Web Token
val token = session.serialize

// You can create a JwtSession from a token of course
JwtSession.deserialize(token)

// You could refresh the session to set its expiration in a few seconds from now
// but you need to set "session.maxAge" in your "application.conf" and since this
// is not a real Play application, we cannot do that, so here, the refresh will do nothing.
session = session.refresh
```

### Using implicits

If you have implicit `Reads` and/or `Writes`, you can access and/or add data directly as case class or object.

```tut
// First, creating the implicits
import play.api.libs.json.Json
import play.api.libs.functional.syntax._

import play.api.Configuration

//In a real Play! App this should normally be injected in the constructor with @Inject()
implicit val conf:Configuration = Configuration.reference

case class User(id: Long, name: String)
implicit val formatUser = Json.format[User]

// Next, adding it to a new session
val session2 = JwtSession() + ("user", User(42, "Paul"))

// Finally, accessing it
session2.getAs[User]("user")
```

## Play RequestHeader

You can extract a `JwtSession` from a `RequestHeader`.

```tut
import pdi.jwt._
import pdi.jwt.JwtSession._
import play.api.test.{FakeRequest, FakeHeaders}

import play.api.Configuration

//In a real Play! App this should normally be injected in the constructor with @Inject()
implicit val conf:Configuration = Configuration.reference

// Default JwtSession
FakeRequest().jwtSession

// What about some headers?
// (the default header for a JSON Web Token is "Authorization" and it should be prefixed by "Bearer ")
val request = FakeRequest().withHeaders(("Authorization", "Bearer " + session2.serialize))
request.jwtSession

// It means you can directly read case classes from the session!
// And that's pretty cool
request.jwtSession.getAs[User]("user")
```

## Play Result

There are also implicit helpers around `Result` to help you manipulate the session inside it.

```tut
// Several functions will need an implicit RequestHeader
// since this is the only way to read the headers of the Result
import play.api.Configuration

//In a real Play! App this should normally be injected in the constructor with @Inject()
implicit val conf:Configuration = Configuration.reference

implicit val implRequest = request

// Let's begin by creating a Result
var result: play.api.mvc.Result = play.api.mvc.Results.Ok

// We can already get a JwtSession from our implicit RequestHeader
result.jwtSession

// Setting a new empty JwtSession
result = result.withNewJwtSession

// Or from an existing JwtSession
result = result.withJwtSession(session2)

// Or from a JsObject
result = result.withJwtSession(Json.obj(("id", 1), ("key", "value")))

// Or from (key, value)
result = result.withJwtSession(("id", 1), ("key", "value"))

// We can add stuff to the current session (only (String, String))
result = result.addingToJwtSession(("key2", "value2"), ("key3", "value3"))

// Or directly classes or objects if you have the correct implicit Writes
result = result.addingToJwtSession("user", User(1, "Paul"))

// Removing from session
result = result.removingFromJwtSession("key2", "key3")

// Refresh the current session
result = result.refreshJwtSession

// So, at the end, you can do
result.jwtSession.getAs[User]("user")
```

## Play configuration

### Secret key

`play.http.secret.key`

> Default: none

The secret key is used to secure cryptographics functions. We are using the same key to sign Json Web Tokens so you don't need to worry about it.

### Session timeout

`play.http.session.maxAge`

> Default: none

Just like for the cookie session, you can use this key to specify the duration, in milliseconds or using the duration syntax (for example 30m or 1h), after which the user should be logout, which mean the token will no longer be valid. It means you need to refresh the expiration date at each request

### Signature algorithm

`play.http.session.algorithm`

> Default: HS256
>
> Supported: HMD5, HS1, HS224, HS256, HS384, HS512

You can specify which algorithm you want to use, among the supported ones, in order to create the signature which will assure you that nobody can actually change the token. You should probably stick with the default one or use HmacSHA512 for maximum security.

### Header name

`play.http.session.jwtName`

> Default: Authorization

You can change the name of the header in which the token should be stored. It will be used for both requests and responses.

### Response header name

`play.http.session.jwtResponseName`

> Default: none

If you need to have a different header for request and response, you can override the response header using this key.


### Token prefix

`play.http.session.tokenPrefix`

> Default: "Bearer "

Authorization header should have a prefix before the token, like "Basic" for example. For a JWT token, it should be "Bearer" (which is the default value) but you can freely change or remove it (using an empty string). The token prefix will be directly prepend before the token, so be sure to put any necessary whitespaces in it.
