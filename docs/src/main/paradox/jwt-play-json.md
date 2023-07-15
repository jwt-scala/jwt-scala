## Play Json

- [API Documentation](https://jwt-scala.github.io/jwt-scala/api/pdi/jwt/JwtJson$.html)

@@@vars

```scala
libraryDependencies += "com.github.jwt-scala" %% "jwt-play-json" % "$project.version$"
```

@@@

### Basic usage

@@snip [JwtPlayJsonDoc.scala](/docs/src/main/scala/JwtPlayJsonDoc.scala) { #example }

### Encoding

@@snip [JwtPlayJsonDoc.scala](/docs/src/main/scala/JwtPlayJsonDoc.scala) { #encode }

### Decoding

@@snip [JwtPlayJsonDoc.scala](/docs/src/main/scala/JwtPlayJsonDoc.scala) { #decode }

### Formating

The project provides implicit reader and writer for both `JwtHeader` and `JwtClaim`

@@snip [JwtPlayJsonDoc.scala](/docs/src/main/scala/JwtPlayJsonDoc.scala) { #format }
