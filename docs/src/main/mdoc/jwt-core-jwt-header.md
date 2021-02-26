## JwtHeader Case Class

```tut
import pdi.jwt.{JwtHeader, JwtAlgorithm}

JwtHeader()
JwtHeader(JwtAlgorithm.HS256)
JwtHeader(JwtAlgorithm.HS256, "JWT")

// You can stringify it to JSON
JwtHeader(JwtAlgorithm.HS256, "JWT").toJson

// You can assign the default type (but it would have be done automatically anyway)
JwtHeader(JwtAlgorithm.HS256).withType
```
