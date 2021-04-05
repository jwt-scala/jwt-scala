import sbt._

object Dependencies {
  object V {
    val scalatest = "3.2.6"
    val plusScalacheck = "3.2.6.0"
    val plusPlay = "5.1.0"
    val bouncyCastle = "1.68"
    val guice = "4.2.3"

    val play = "2.8.7"
    val playJson = "2.9.2"
    val json4s = "3.6.11"
    val circe = "0.13.0"
    val upickle = "1.3.9"
    val sprayJson = "1.3.6"
    val argonaut = "6.3.3"
  }

  object Libs {
    val play = "com.typesafe.play" %% "play" % V.play
    val playJson = "com.typesafe.play" %% "play-json" % V.playJson
    val playTest = "com.typesafe.play" %% "play-test" % V.play % "test"
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play
    val guice = "com.google.inject" % "guice" % V.guice % "test"

    val json4sCore = "org.json4s" %% "json4s-core" % V.json4s
    val json4sNative = "org.json4s" %% "json4s-native" % V.json4s
    val json4sJackson = "org.json4s" %% "json4s-jackson" % V.json4s

    val circeCore = "io.circe" %% "circe-core" % V.circe
    val circeGeneric = "io.circe" %% "circe-generic" % V.circe
    val circeParse = "io.circe" %% "circe-parser" % V.circe

    val upickle = "com.lihaoyi" %% "upickle" % V.upickle

    val sprayJson = "io.spray" %% "spray-json" % V.sprayJson

    val argonaut = "io.argonaut" %% "argonaut" % V.argonaut

    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle % "test"
    val bouncyCastleTut = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest % "test"
    val scalacheck = "org.scalatestplus" %% "scalacheck-1-15" % V.plusScalacheck % "test"
    val scalatestPlus = "org.scalatestplus.play" %% "scalatestplus-play" % V.plusPlay % "test"
  }
}
