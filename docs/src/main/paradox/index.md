# JWT Scala

@@@ index

- [Native](jwt-core/index.md)
- [Argonaut](jwt-argonaut.md)
- [Circe](jwt-circe.md)
- [Json4S](jwt-json4s.md)
- [Play Json](jwt-play-json.md)
- [Play Framework](jwt-play-jwt-session.md)
- [upickle](jwt-upickle.md)
- [ZIO Json](jwt-zio-json.md)

@@@

Scala support for JSON Web Token ([JWT](http://tools.ietf.org/html/draft-ietf-oauth-json-web-token)).
Supports Java 8+, Scala 2.12, Scala 2.13 and Scala 3 (for json libraries that support it).
Dependency free.
Optional helpers for Play Framework, Play JSON, Json4s Native, Json4s Jackson, Circe, uPickle and Argonaut.

## Usage

JWT Scala is divided in several sub-projects each targeting a specific JSON library,
check the doc from the menu for installation and usage instructions.

## Algorithms

If you are using `String` key, please keep in mind that such keys need to be parsed. Rather than implementing a super complex parser, the one in JWT Scala is pretty simple and might not work for all use-cases (especially for ECDSA keys). In such case, consider using `SecretKey` or `PrivateKey` or `PublicKey` directly. It is way better for you. All API support all those types.

Check @ref:[ECDSA samples](jwt-core/jwt-ecdsa.md) for more infos.

| Name  | Description                    |
| ----- | ------------------------------ |
| HMD5  | HMAC using MD5 algorithm       |
| HS224 | HMAC using SHA-224 algorithm   |
| HS256 | HMAC using SHA-256 algorithm   |
| HS384 | HMAC using SHA-384 algorithm   |
| HS512 | HMAC using SHA-512 algorithm   |
| RS256 | RSASSA using SHA-256 algorithm |
| RS384 | RSASSA using SHA-384 algorithm |
| RS512 | RSASSA using SHA-512 algorithm |
| ES256 | ECDSA using SHA-256 algorithm  |
| ES384 | ECDSA using SHA-384 algorithm  |
| ES512 | ECDSA using SHA-512 algorithm  |
| EdDSA | EdDSA signature algorithms     |

## Security concerns

This lib doesn't want to impose anything, that's why, by default, a JWT claim is totally empty. That said, you should always add an `issuedAt` attribute to it, probably using `claim.issuedNow`.
The reason is that even HTTPS isn't perfect and having always the same chunk of data transfered can be of a big help to crack it. Generating a slightly different token at each request is way better even if it adds a bit of payload to the response.
If you are using a session timeout through the `expiration` attribute which is extended at each request, that's fine too. I can't find the article I read about that vulnerability but if someone has some resources about the topic, I would be glad to link them.

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright 2021 JWT-Scala Contributors.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
