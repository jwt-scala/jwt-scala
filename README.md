# JWT Scala 0.0.3

[JSON Web Token (JWT)](http://tools.ietf.org/html/draft-ietf-oauth-json-web-token) is a compact URL-safe means of representing claims to be transferred between two parties. The claims in a JWT are encoded as a JSON object that is digitally signed using JSON Web Signature (JWS). [IETF](http://tools.ietf.org/html/draft-ietf-oauth-json-web-token)

This project aims to provide a set of libs to support **JWT in Scala** and its ecosystem. It supports **Java 1.6+** and both **Scala 2.10.x and Scala 2.11.x**. It has **no dependency** except if you are using Java 1.6 or 1.7 (which have both reach end-of-life by the way) to Apache Commons Codec to grab a decent Base64 encoder / decoder.

## Pick your lib

Depending on your project, you need to pick the right lib to use. You will probably need to include only one of them, mostly because their are build in top of one another. So, just click the right answer to the next question, and I will tell you which lib to use.

### What is your project using?

- [jwt-core](#jwt-core) - Only pure Scala
- [jwt-play-json](#jwt-play-json) - The `play-json` lib but not the `play framework` itself
- [jwt-play](#jwt-play) - Play Framework (with `play-json`)
- The `json4s` lib (coming soon)
- Play Framework (with `json4s`) (coming soon)

### Give me some code samples!

You can find them inside the online doc.

- [jwt-core](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-core/index.html#pdi.jwt.Jwt$)
- [jwt-play-json](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-play-json/index.html#pdi.jwt.JwtJson$)
- [jwt-play](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-play/index.html#pdi.jwt.JwtPlayImplicits)
- [Full Play Framework application](https://github.com/pauldijou/jwt-scala/tree/master/examples/play-angular-standalone)

## Dependencies

- `"org.bouncycastle" % "bcpkix-jdk15on"`
- `"commons-codec" % "commons-codec"` (only if Java 1.6 or 1.7)

## Which Java?

All libs in this project have 2 versions. One target Java 1.8+, using the new Time API and the new Base64 util. This is the default one. If you are using Java 1.6 or 1.7, you will have to use the "legacy" version of the lib. It's exactly the same (in fact, 99% of the code source is shared) except it's using an old Calendar API and the Base64 util from Apache Commons Codec.

The naming convention is to add `-legacy` to the dependency name to grab the "legacy" version of it. For example, `jwt-core` is targeting Java 1.8+ while `jwt-core-legacy` is for environment running Java 1.6 or 1.7.

## Got a problem?

If you found any bug or need more documentation, immediately feel an [issue in GitHub](https://github.com/pauldijou/jwt-scala/issues). I should read most of them.

If you want to submit a PR to improve the project, that would be awesome. If, in top of that, you want to run the tests (yeah, because there are tests! A few of them...) before submitting it, you are just amasing but also don't run the tests globally. If you do `sbt test`, the whole universe will nearly collapse due to classpath errors that are totally beyond the understanding of the current me. Run them by project, like `playEdge/test` inside an `sbt` console. And even so, they might fail because of a `LinkageError`, just run them twice and it will work... I don't know why... I drank too much whisky trying to understand it so I just stopped. If you are a classpath God, just ping me and I will have questions for you.

## Projects

### jwt-core

[API](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-core/)

Low-level API mostly based on String and Map since there is no native support for JSON in Scala.

**build.sbt, Java 1.8+**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-core" % "0.0.3"
)
~~~

**build.sbt, Java 1.6 and 1.7**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-core-legacy" % "0.0.3"
)
~~~

### jwt-play-json

[API](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-play-json/)

Nice API to interact with JWT using JsObject from the Play JSON lib.

**build.sbt, Java 1.8+**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play-json" % "0.0.3"
)
~~~

**build.sbt, Java 1.6 and 1.7**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play-json-legacy" % "0.0.3"
)
~~~

### jwt-play

[API](http://pauldijou.fr/jwt-scala-doc/api/0.0.3/jwt-play/)

Built in top of Play JSON, extend the `Result` class in order to allow you to manage the `Session` using JWT.

**build.sbt, Java 1.8+**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play" % "0.0.3"
)
~~~

**build.sbt, Java 1.6 and 1.7**
~~~
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play-legacy" % "0.0.3"
)
~~~

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2015 Paul Dijou (http://pauldijou.fr).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
