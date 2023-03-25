import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Dependencies {
  object V {
    val munit = "0.7.29"
    val bouncyCastle = "1.72"
    val guice = "4.2.3"

    val scalaJavaTime = "2.5.0"
    val scalajsSecureRandom = "1.0.0"
    val play = "2.8.19"
    val playJson = "2.9.4"
    val json4s = "4.0.6"
    val circe = "0.14.5"
    val upickle = "3.1.0"
    val zioJson = "0.5.0"
    val argonaut = "6.3.8"
  }

  object Libs {
    val scalaJavaTime = Def.setting("io.github.cquiroz" %%% "scala-java-time" % V.scalaJavaTime)
    val scalajsSecureRandom = Def.setting(
      ("org.scala-js" %%% "scalajs-java-securerandom" % V.scalajsSecureRandom).cross(
        CrossVersion.for3Use2_13
      )
    )

    val play = "com.typesafe.play" %% "play" % V.play
    val playJson = "com.typesafe.play" %% "play-json" % V.playJson
    val playTest = "com.typesafe.play" %% "play-test" % V.play % Test
    val playTestProvided = "com.typesafe.play" %% "play-test" % V.play
    val guice = "com.google.inject" % "guice" % V.guice % "test"

    val json4sCore = "org.json4s" %% "json4s-core" % V.json4s
    val json4sNative = "org.json4s" %% "json4s-native-core" % V.json4s
    val json4sJackson = "org.json4s" %% "json4s-jackson-core" % V.json4s

    val circeCore = Def.setting("io.circe" %%% "circe-core" % V.circe)
    val circeGeneric = Def.setting("io.circe" %%% "circe-generic" % V.circe)
    val circeJawn = Def.setting("io.circe" %%% "circe-jawn" % V.circe)
    val circeParse = Def.setting("io.circe" %%% "circe-parser" % V.circe)

    val upickle = "com.lihaoyi" %% "upickle" % V.upickle

    val zioJson = "dev.zio" %% "zio-json" % V.zioJson

    val argonaut = "io.argonaut" %% "argonaut" % V.argonaut

    val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk18on" % V.bouncyCastle % Test
    val bouncyCastleTut = "org.bouncycastle" % "bcpkix-jdk18on" % V.bouncyCastle

    val munit = Def.setting("org.scalameta" %%% "munit" % V.munit % Test)
    val munitScalacheck = Def.setting("org.scalameta" %%% "munit-scalacheck" % V.munit % Test)
  }
}
