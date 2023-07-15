## JwtSession case class

@@@vars

```scala
libraryDependencies += "com.github.jwt-scala" %% "jwt-play" % "$project.version$"
```

@@@

Provides an API similar to the Play [Session](https://www.playframework.com/documentation/2.3.x/api/scala/index.html#play.api.mvc.Session) but using `JsValue` rather than `String` as values. It also separates `headerData` from `claimData` rather than having only one `data`.

### Basic usage

@@snip [JwtPlayJwtSessionDoc.scala](/docs/src/main/scala/JwtPlayJwtSessionDoc.scala) { #example }

### Using implicits

If you have implicit `Reads` and/or `Writes`, you can access and/or add data directly as case class or object.

@@snip [JwtPlayJwtSessionDoc.scala](/docs/src/main/scala/JwtPlayJwtSessionDoc.scala) { #implicits }

## Play RequestHeader

You can extract a `JwtSession` from a `RequestHeader`.

@@snip [JwtPlayJwtSessionDoc.scala](/docs/src/main/scala/JwtPlayJwtSessionDoc.scala) { #requestheader }

## Play Result

There are also implicit helpers around `Result` to help you manipulate the session inside it.

@@snip [JwtPlayJwtSessionDoc.scala](/docs/src/main/scala/JwtPlayJwtSessionDoc.scala) { #result }

## Play configuration

### Secret key

`play.http.secret.key`

> Default: none

The secret key is used to secure cryptographics functions. We are using the same key to sign Json Web Tokens so you don't need to worry about it.

### Private key

`play.http.session.privateKey`

> Default: none

The PKCS8 format private key is used to sign JWT session. If `play.http.session.privateKey` is missing `play.http.secret.key` used instead.

### Public key

`play.http.session.publicKey`

> Default: none

The X.509 format public key is used to verify JWT session signed with private key `play.http.session.privateKey`

### Session timeout

`play.http.session.maxAge`

> Default: none

Just like for the cookie session, you can use this key to specify the duration, in milliseconds or using the duration syntax (for example 30m or 1h), after which the user should be logout, which mean the token will no longer be valid. It means you need to refresh the expiration date at each request

### Signature algorithm

`play.http.session.algorithm`

> Default: HS256
>
> Supported: HMD5, HS1, HS224, HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512

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
