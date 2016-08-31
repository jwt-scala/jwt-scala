import sbt._

object Dependencies {
  object V {
    val play = "2.5.0"
    val json4s = "3.3.0"
    val circe = "0.5.0"
    val scalatest = "2.2.6"
    val scalatestPlus = "1.4.0"
    val jmockit = "1.24"
    val apacheCodec = "1.10"
    val bouncyCastle = "1.52"
  }

  object Libs {
    val play             = "com.typesafe.play" %% "play"      % V.play % "provided"
    val playJson         = "com.typesafe.play" %% "play-json" % V.play % "provided"
    val playTest         = "com.typesafe.play" %% "play-test" % V.play % "test"
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play % "provided"

    val json4sCore    = "org.json4s" %% "json4s-core"    % V.json4s % "provided"
    val json4sNative  = "org.json4s" %% "json4s-native"  % V.json4s % "provided"
    val json4sJackson = "org.json4s" %% "json4s-jackson" % V.json4s % "provided"

    val circeCore     = "io.circe" %% "circe-core"    % V.circe
    val circeGeneric  = "io.circe" %% "circe-generic" % V.circe
    val circeParse    = "io.circe" %% "circe-parser"  % V.circe

    val apacheCodec = "commons-codec" % "commons-codec" % V.apacheCodec
    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest % "test"
    val scalatestPlus = "org.scalatestplus" %% "play" % V.scalatestPlus % "test"
    val jmockit = "org.jmockit" % "jmockit" % V.jmockit % "test"
  }
}
