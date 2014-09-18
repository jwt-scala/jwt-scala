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
