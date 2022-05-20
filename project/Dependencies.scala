import sbt._

object Dependencies {
  object V {
    val munit = "0.7.29"
    val bouncyCastle = "1.70"
    val guice = "4.2.3"

    val play = "2.8.15"
    val playJson = "2.9.2"
    val json4s = "4.0.5"
    val circe = "0.14.2"
    val upickle = "1.6.0"
    val argonaut = "6.3.8"
  }

  object Libs {
    val play = "com.typesafe.play" %% "play" % V.play
    val playJson = "com.typesafe.play" %% "play-json" % V.playJson
    val playTest = "com.typesafe.play" %% "play-test" % V.play % Test
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play
    val guice = "com.google.inject" % "guice" % V.guice % "test"

    val json4sCore = "org.json4s" %% "json4s-core" % V.json4s
    val json4sNative = "org.json4s" %% "json4s-native-core" % V.json4s
    val json4sJackson = "org.json4s" %% "json4s-jackson-core" % V.json4s

    val circeCore = "io.circe" %% "circe-core" % V.circe
    val circeGeneric = "io.circe" %% "circe-generic" % V.circe
    val circeParse = "io.circe" %% "circe-parser" % V.circe

    val upickle = "com.lihaoyi" %% "upickle" % V.upickle

    val argonaut = "io.argonaut" %% "argonaut" % V.argonaut

    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle % Test
    val bouncyCastleTut = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle

    val munit = "org.scalameta" %% "munit" % V.munit % Test
    val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % V.munit % Test
  }
}
