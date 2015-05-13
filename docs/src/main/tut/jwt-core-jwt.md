## Jwt object

### Basic usage

```tut
import pdi.jwt.{Jwt, JwtAlgorithm, JwtHeader, JwtClaim}
val token = Jwt.encode("""{"user":1}""", "secretKey", JwtAlgorithm.HS256)
Jwt.decodeRawAll(token, Option("secretKey"))
Jwt.decodeRawAll(token, Option("wrongKey"))
```

### Encoding

```tut
// Encode from string, header automatically generated
Jwt.encode("""{"user":1}""","secretKey", JwtAlgorithm.HS384)

// Encode from case class, header automatically generated
// Set that the token has been issued now and expires in 10 seconds
Jwt.encode(JwtClaim({"""{"user":1}"""}).issuedNow.expiresIn(10), "secretKey", JwtAlgorithm.HS512)

// You can encode without signing it
Jwt.encode("""{"user":1}""")

// You can specify a string header but also need to specify the algorithm just to be sure
// This is not really typesafe, so please use it with care
Jwt.encode("""{"typ":"JWT","alg":"HS1"}""", """{"user":1}""", "key", JwtAlgorithm.HS1)

// If using a case class header, no need to repeat the algorithm
// This is way better than the previous one
Jwt.encode(JwtHeader(JwtAlgorithm.HS1), JwtClaim("""{"user":1}"""), "key")
```

### Decoding

In JWT Scala, espcially when using raw strings which are not typesafe at all, there are a lot of possible errors. This is why nearly all `decode` functions will return a `Try` rather than directly the expected result. In case of failure, the wrapped exception should tell you what went wront.

```tut
// Decode all parts of the token as string
Jwt.decodeRawAll(token, Option("secretKey"))

// Decode only the claim as a string
Jwt.decodeRaw(token, Option("secretKey"))

// Decode all parts and cast them as a better type if possible.
// Since the implementation in JWT Core only use string, it is the same as decodeRawAll
// But check the result in JWT Play JSON to see the difference
Jwt.decodeAll(token, Option("secretKey"))

// Same as before, but only the claim
// (you should start to see a pattern in the naming convention of the functions)
Jwt.decode(token, Option("secretKey"))

// Failure because the token is not a token at all
Jwt.decode("Hey there!")

// Failure if not Base64 encoded
Jwt.decode("a.b.c")

// Failure in case we use the wrong key
Jwt.decode(token, Option("wrongKey"))

// Failure if the token only starts in 5 seconds
Jwt.decode(Jwt.encode(JwtClaim().startsIn(5)))
```

### Validating

If you only want to check if a token is valid without decoding it. You have two options: `validate` functions that will throw the exceptions we saw in the decoding section, so you know what went wrong, or `isValid` functions that will return a boolean in case you don't care about the actual error and don't want to bother with catching exception.

```tut:nofail
// All good
Jwt.validate(token, "secretKey")
Jwt.isValid(token, "secretKey")

// Wrong key here
Jwt.validate(token, "wrongKey")
Jwt.isValid(token, "wrongKey")

// No key for unsigned token => ok
Jwt.validate(Jwt.encode("{}"))
Jwt.isValid(Jwt.encode("{}"))

// No key while the token is actually signed => wrong
Jwt.validate(token)
Jwt.isValid(token)

// The token hasn't started yet!
Jwt.validate(Jwt.encode(JwtClaim().startsIn(5)))
Jwt.isValid(Jwt.encode(JwtClaim().startsIn(5)))

// This is no token
Jwt.validate("a.b.c")
Jwt.isValid("a.b.c")
```
