import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Versions {
  val munit = "1.0.0-M10"

  val bouncyCastle = "1.76"

  val guice = "4.2.3"

  val scalaJavaTime = "2.5.0"

  val scalajsSecureRandom = "1.0.0"

  val play = "3.0.0"

  val playJson = "2.10.2"

  val json4s = "4.0.6"

  val circe = "0.14.6"

  val upickle = "3.1.3"

  val zioJson = "0.6.2"

  val argonaut = "6.3.9"
}

object Libs {
  val scalaJavaTime =
    Def.setting("io.github.cquiroz" %%% "scala-java-time" % Versions.scalaJavaTime)
  val scalajsSecureRandom = Def.setting(
    ("org.scala-js" %%% "scalajs-java-securerandom" % Versions.scalajsSecureRandom).cross(
      CrossVersion.for3Use2_13
    )
  )

  val play = "org.playframework" %% "play" % Versions.play
  val playJson = "com.typesafe.play" %% "play-json" % Versions.playJson
  val playTest = "org.playframework" %% "play-test" % Versions.play % Test
  val playTestProvided = "org.playframework" %% "play-test" % Versions.play
  val guice = "com.google.inject" % "guice" % Versions.guice % "test"

  val json4sCore = "org.json4s" %% "json4s-core" % Versions.json4s
  val json4sNative = "org.json4s" %% "json4s-native-core" % Versions.json4s
  val json4sJackson = "org.json4s" %% "json4s-jackson-core" % Versions.json4s

  val circeCore = Def.setting("io.circe" %%% "circe-core" % Versions.circe)
  val circeGeneric = Def.setting("io.circe" %%% "circe-generic" % Versions.circe)
  val circeJawn = Def.setting("io.circe" %%% "circe-jawn" % Versions.circe)
  val circeParse = Def.setting("io.circe" %%% "circe-parser" % Versions.circe)

  val upickle = Def.setting("com.lihaoyi" %%% "upickle" % Versions.upickle)

  val zioJson = "dev.zio" %% "zio-json" % Versions.zioJson

  val argonaut = "io.argonaut" %% "argonaut" % Versions.argonaut

  val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk18on" % Versions.bouncyCastle % Test
  val bouncyCastleTut = "org.bouncycastle" % "bcpkix-jdk18on" % Versions.bouncyCastle

  val munit = Def.setting("org.scalameta" %%% "munit" % Versions.munit % Test)
  val munitScalacheck =
    Def.setting("org.scalameta" %%% "munit-scalacheck" % Versions.munit % Test)
}
