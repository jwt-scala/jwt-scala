import sbt._

object Dependencies {
  object V {
    val play = "2.4.0"
    val json4s = "3.2.10"
    val scalatest = "2.2.4"
    val scalatestPlus = "1.2.0"
    val jmockit = "1.17"
    val apacheCodec = "1.10"
    val bouncyCastle = "1.52"
  }

  object Libs {
    val play             = "com.typesafe.play" %% "play"      % V.play % "provided"
    val playJson         = "com.typesafe.play" %% "play-json" % V.play % "provided"
    val playTest         = "com.typesafe.play" %% "play-test" % V.play % "test"
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play % "provided"
    // This one is to fix a DI bug during test
    val playWs           = "com.typesafe.play" %% "play-ws"   % V.play % "test"

    val json4sCore    = "org.json4s" %% "json4s-core"    % V.json4s % "provided"
    val json4sNative  = "org.json4s" %% "json4s-native"  % V.json4s % "provided"
    val json4sJackson = "org.json4s" %% "json4s-jackson" % V.json4s % "provided"

    val apacheCodec = "commons-codec" % "commons-codec" % V.apacheCodec
    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest % "test"
    val scalatestPlus = "org.scalatestplus" %% "play" % V.scalatestPlus % "test"
    val jmockit = "org.jmockit" % "jmockit" % V.jmockit % "test"
  }
}
