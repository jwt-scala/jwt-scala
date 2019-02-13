import sbt._

object Dependencies {
  object V {
    val scalatest = "3.0.5"
    val scalatestPlus = "4.0.0"
    val jmockit = "1.24"
    val apacheCodec = "1.10"
    val bouncyCastle = "1.60"
    val guice = "4.2.2"

    val play = "2.7.0"
    val playJson = "2.7.1"
    val json4s = "3.6.4"
    val circe = "0.11.1"
    val upickle = "0.7.1"
    val sprayJson = "1.3.4"
    val argonaut = "6.2.2"
  }

  object Libs {
    val play             = "com.typesafe.play" %% "play"      % V.play
    val playJson         = "com.typesafe.play" %% "play-json" % V.playJson
    val playTest         = "com.typesafe.play" %% "play-test" % V.play  % "test"
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play
    val guice            = "com.google.inject" %  "guice"     % V.guice % "test"

    val json4sCore    = "org.json4s" %% "json4s-core"    % V.json4s
    val json4sNative  = "org.json4s" %% "json4s-native"  % V.json4s
    val json4sJackson = "org.json4s" %% "json4s-jackson" % V.json4s

    val circeCore     = "io.circe" %% "circe-core"    % V.circe
    val circeGeneric  = "io.circe" %% "circe-generic" % V.circe
    val circeParse    = "io.circe" %% "circe-parser"  % V.circe

    val upickle = "com.lihaoyi" %% "upickle" % V.upickle

    val sprayJson = "io.spray" %%  "spray-json" % V.sprayJson

    val argonaut = "io.argonaut" %% "argonaut" % V.argonaut

    val apacheCodec = "commons-codec" % "commons-codec" % V.apacheCodec
    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest % "test"
    val scalatestPlus = "org.scalatestplus.play" %% "scalatestplus-play" % V.scalatestPlus % "test"
    val jmockit = "org.jmockit" % "jmockit" % V.jmockit % "test"
  }
}
