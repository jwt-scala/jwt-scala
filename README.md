# JWT Scala 0.0.4

Scala support for JSON Web Token ([JWT](http://tools.ietf.org/html/draft-ietf-oauth-json-web-token)). Supports Java 1.6+, Scala 2.10.x and Scala 2.11.x. Optional helpers for Play Framework and Play JSON.

## Pick the right tool for the right job

JWT Scala is divided in several sub-projects each targeting a specific use-case. Depending on your need, you want to pick the right one.

| Name | Description | Samples | Scaladoc |
|------|-------------|---------|----------|
|`jwt-core`|Pure Scala|[Jwt](https://pauldijou.github.io/jwt-scala/samples/jwt-core)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-core)|
|`jwt-play-json`|`play-json` lib|[JwtJson](https://pauldijou.github.io/jwt-scala/samples/jwt-play-json)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-play-json)|
|`jwt-play`|Play framework|[JwtSession](https://pauldijou.github.io/jwt-scala/samples/jwt-play)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-play)|

## Install

In the following snippet, replace `[name]` with the actual name of the project you need. **Using Java 1.6 or 1.7?** Add `-legacy` after the name of the project. See [below](#which-java) why.

**build.sbt**

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "[name]" % "0.0.4"
)
```

### Example for `jwt-play` using Java 1.6

**build.sbt**

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play-legacy" % "0.0.4"
)
```

## Dependencies

- `"org.bouncycastle" % "bcpkix-jdk15on"`
- `"commons-codec" % "commons-codec"` (only if Java 1.6 or 1.7)

## Algorithms

If the algorithm is tagged as `Exp.` (experimental), it means it might work, but probably not, but we are working hard to do so (please help if you can).

|Name|Alias|Description|Exp.|
|----|-----|-----------|----|
|HMD5|HmacMD5|HMAC using MD5 algorithm|✗|
|HS1|HmacSHA1|HMAC using SHA-1 algorithm|✗|
|HS224|HmacSHA224|HMAC using SHA-224 algorithm|✗|
|HS256|HmacSHA256|HMAC using SHA-256 algorithm|✗|
|HS384|HmacSHA384|HMAC using SHA-384 algorithm|✗|
|HS512|HmacSHA512|HMAC using SHA-512 algorithm|✗|
|RS1|RSASHA1|RSASSA using SHA-1 algorithm|✗|
|RS256|RSASHA256|RSASSA using SHA-256 algorithm|✗|
|RS384|RSASHA384|RSASSA using SHA-384 algorithm|✗|
|RS512|RSASHA512|RSASSA using SHA-512 algorithm|✗|
|ES1|ECDSASHA1|ECDSA using SHA-1 algorithm|✔|
|ES256|ECDSASHA256|ECDSA using SHA-256 algorithm|✔|
|ES384|ECDSASHA384|ECDSA using SHA-384 algorithm|✔|
|ES512|ECDSASHA512|ECDSA using SHA-512 algorithm|✔|

## Which Java?

Actually, all sub-projects have two flavours. One target Java 1.8+, using the new Time API and the new Base64 util. This is the default one. If you are using Java 1.6 or 1.7 (which have both reach end-of-life by the way), you will have to use the "legacy" version of the lib. It's exactly the same (in fact, 99% of the code source is shared) except it's using the old Calendar API and the Base64 util from Apache Commons Codec (introducing **one small dependency** in this particular use-case).

## Got a problem?

If you found any bug or need more documentation, immediately feel an [issue in GitHub](https://github.com/pauldijou/jwt-scala/issues). I should read most of them.

If you want to submit a PR to improve the project, that would be awesome. If, in top of that, you want to run the tests (yeah, because there are tests! A few of them...) before submitting it, you are just amasing but also don't run the tests globally. If you do `sbt test`, the whole universe will nearly collapse due to classpath errors that are totally beyond the understanding of the current me. Run them by project, like `playEdge/test` inside an `sbt` console. And even so, they might fail because of a `LinkageError`, just run them twice and it will work... I don't know why... I drank too much whisky trying to understand it so I just stopped. If you are a classpath God, just ping me and I will have questions for you.

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2015 Paul Dijou ([http://pauldijou.fr](http://pauldijou.fr)).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
