import sbt._

object Dependencies {
  object V {
    val play = "2.3.3"
    val json4s = "3.2.10"
    val scalatest = "2.2.4"
    val jmockit = "1.17"
    val apacheCodec = "1.10"
  }

  object Libs {
    val play     = "com.typesafe.play" %% "play"      % V.play
    val playJson = "com.typesafe.play" %% "play-json" % V.play

    val json4sNative = "org.json4s" %% "json4s-native"   % V.json4s
    val json4sJackson = "org.json4s" %% "json4s-jackson" % V.json4s

    val apacheCodec = "commons-codec" % "commons-codec" % V.apacheCodec

    val scalatest = "org.scalatest" %% "scalatest" % V.scalatest % "test"
    val jmockit = "org.jmockit" % "jmockit" % V.jmockit % "test"
  }
}
