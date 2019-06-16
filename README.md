# JWT Scala 3.0.1

Scala support for JSON Web Token ([JWT](http://tools.ietf.org/html/draft-ietf-oauth-json-web-token)). Supports Java 8+, Scala 2.11.x, Scala 2.12.x and Scala 2.13.0-M5. Dependency free. Optional helpers for Play Framework, Play JSON, Json4s Native, Json4s Jackson, Circe, uPickle, Spray JSON and Argonaut.

## Pick the right tool for the right job

JWT Scala is divided in several sub-projects each targeting a specific use-case. Depending on your need, you want to pick the right one.

| Name | Description | Samples | Scaladoc |
|------|-------------|---------|----------|
|`jwt-core`|Pure Scala|[Jwt](https://pauldijou.github.io/jwt-scala/samples/jwt-core)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-core)|
|`jwt-play-json`|[play-json](https://www.playframework.com/) lib|[JwtJson](https://pauldijou.github.io/jwt-scala/samples/jwt-play-json)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-play-json)|
|`jwt-play`|[Play framework](https://www.playframework.com/)|[JwtSession](https://pauldijou.github.io/jwt-scala/samples/jwt-play)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-play)|
|`jwt-json4s-native`|[json4s](http://json4s.org/) Native implementation|[JwtJson4s](https://pauldijou.github.io/jwt-scala/samples/jwt-json4s)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-json4s)|
|`jwt-json4s-jackson`|[json4s](http://json4s.org/) Jackson implementation|[JwtJson4s](https://pauldijou.github.io/jwt-scala/samples/jwt-json4s)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-json4s)|
|`jwt-spray-json`|[spray-json](https://github.com/spray/spray-json) lib|[JwtSprayJson](https://pauldijou.github.io/jwt-scala/samples/jwt-spray-json)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-spray-json)|
|`jwt-circe`|[circe](https://circe.github.io/circe/) lib|[JwtCirce](https://pauldijou.github.io/jwt-scala/samples/jwt-circe)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-circe)|
|`jwt-upickle`|[uPickle](http://www.lihaoyi.com/upickle-pprint/upickle/) lib|[JwtUpickle](https://pauldijou.github.io/jwt-scala/samples/jwt-upickle)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-upickle)|
|`jwt-argonaut`|[Argonaut](http://argonaut.io/) lib|[JwtArgonaut](https://pauldijou.github.io/jwt-scala/samples/jwt-argonaut)|[API](https://pauldijou.github.io/jwt-scala/api/latest/jwt-argonaut)|

If you need a previous version of the Scaladoc API, check [the bottom of this page](https://pauldijou.github.io/jwt-scala/api/#old-apis)

You can also check a [standalone Play application](https://github.com/pauldijou/jwt-scala/tree/master/examples/play-angular-standalone) using `jwt-play` and implementating a small REST API with authentication and admin role (include a UI too!).

## Install

In the following snippet, replace `[name]` with the actual name of the project you need. **Using Java 1.6 or 1.7?** Add `-legacy` after the name of the project and use a version before 2.0.0. See [below](#which-java) why.

**build.sbt**

```scala
libraryDependencies ++= Seq(
  "com.pauldijou" %% "[name]" % "3.0.1"
)
```

### Example for `jwt-play` using Java 1.6

**build.sbt**

```scala
libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-play-legacy" % "1.1.0"
)
```

## Dependencies

None

## Algorithms

If you are using `String` key, please keep in mind that such keys need to be parsed. Rather than implementing a super complex parser, the one in JWT Scala is pretty simple and might not work for all use-cases (especially for ECDSA keys). In such case, consider using `SecretKey` or `PrivateKey` or `PublicKey` directly. It is way better for you. All API support all those types.

Check [ECDSA samples](https://pauldijou.github.io/jwt-scala/samples/jwt-ecdsa) for more infos.

|Name|Description|
|----|-----------|
|HMD5|HMAC using MD5 algorithm|
|HS224|HMAC using SHA-224 algorithm|
|HS256|HMAC using SHA-256 algorithm|
|HS384|HMAC using SHA-384 algorithm|
|HS512|HMAC using SHA-512 algorithm|
|RS256|RSASSA using SHA-256 algorithm|
|RS384|RSASSA using SHA-384 algorithm|
|RS512|RSASSA using SHA-512 algorithm|
|ES256|ECDSA using SHA-256 algorithm|
|ES384|ECDSA using SHA-384 algorithm|
|ES512|ECDSA using SHA-512 algorithm|

## <a name="which-java"></a>Which Java?

**Java 6 and 7 support has been removed with version 2.0.0**

Actually, all sub-projects have two flavours. One target Java 8+, using the new Time API and the new Base64 util. This is the default one. If you are using Java 1.6 or 1.7 (which have both reached end-of-life by the way), you will have to use the "legacy" version of the lib. It's exactly the same (in fact, 99% of the code source is shared) except it's using the old Calendar API and the Base64 util from Apache Commons Codec (introducing **one small dependency** in this particular use-case).

## Security concerns

This lib doesn't want to impose anything, that's why, by default, a JWT claim is totally empty. That said, you should always add an `issuedAt` attribute to it, probably using `claim.issuedNow`. The reason is that even HTTPS isn't perfect and having always the same chunk of data transfered can be of a big help to crack it. Generating a slightly different token at each request is way better even if it adds a bit of payload to the response. If you are using a session timeout through the `expiration` attribute which is extended at each request, that's fine too. I can't find the article I read about that vulnerability but if someone has some resources about the topic, I would be glad to link them.

## Got a problem?

If you found any bug or need more documentation, immediately feel an [issue in GitHub](https://github.com/pauldijou/jwt-scala/issues). I should read most of them.

If you want to submit a PR to improve the project, that would be awesome. If, in top of that, you want to run the tests (yeah, because there are tests! A few of them...) before submitting it, you are just amasing but also don't run the tests globally. If you do `sbt test`, the whole universe will nearly collapse due to classpath errors that are totally beyond the understanding of the current me. Run them by project, like `playEdge/test` inside an `sbt` console. And even so, they might fail because of a `LinkageError`, just run them twice and it will work... I don't know why... I drank too much whisky trying to understand it so I just stopped. If you are a classpath God, just ping me and I will have questions for you.

## Notes

- **Test**: run all tests with `sbt testAll` (if `java.lang.LinkageError`, just re-run the command)
- **Publish**: update version numbers in `build.sbt` and run `sbt release` (be sure to either `reload` inside sbt or start a new sbt)
- **Scaladoc**: to manually generate all scaladoc, run `sbt scaladoc`
- **Publish docs**: to manually build and push online the doc website, run `sbt publish-doc`
- **Docs**: to have a locally running doc website:
  - `sbt ~docs/makeSite`
  - `cd docs/target/site`
  - `jekyll serve`
  - Go to [http://localhost:4000/jwt-scala/](http://localhost:4000/jwt-scala/)

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2015 Paul Dijou ([http://pauldijou.fr](http://pauldijou.fr)).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
