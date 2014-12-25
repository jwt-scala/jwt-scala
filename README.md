# Scala JWT

[JWT](https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-25) support for Scala. Pick the best project depending on your need...

## Core

Project: [scala-jwt-core](https://github.com/pauldijou/jwt-scala/tree/master/scala-jwt-core)

Low-level API based mostly based on String and Map since there is no native support for JSON in Scala.

## Play JSON

Project: [scala-jwt-play-json](https://github.com/pauldijou/jwt-scala/tree/master/scala-jwt-play-json)

Nice API to interact with JWT using JsObject from the Play JSON lib.

## Play

Project: [scala-jwt-play](https://github.com/pauldijou/jwt-scala/tree/master/scala-jwt-play)

Built in top of Play JSON, extend the `Result` class in order to allow you to manage the `Session` using JWT.

## Json4s

Work in progress...

## Examples

- [Play Framework + AngularJS](https://github.com/pauldijou/jwt-scala/tree/master/examples/play-angular)

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2015 Paul Dijou (http://pauldijou.fr).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
