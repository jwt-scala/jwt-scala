## JWT object

```tut
import pdi.jwt._
Jwt.encode("""{"alg":"HS256","typ":"JWT"}""","""{user:1}""","secretKey", JwtAlgorithm.HS256)
```
