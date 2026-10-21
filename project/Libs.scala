import sbt.{given, *}

object Versions {
  val munit = "1.1.0"

  val bouncyCastle = "1.86"

  val guice = "4.2.3"

  val scalaJavaTime = "2.6.0"

  val scalajsSecureRandom = "1.0.0"

  val play = "3.0.9"

  val playJson = "3.0.5"

  val json4s = "4.0.7"

  val circe = "0.14.14"

  val upickle = "4.4.0"

  val zioJson = "0.7.44"

  val argonaut = "6.3.10"
}

object Libs {
  val scalaJavaTime = "io.github.cquiroz" %% "scala-java-time" % Versions.scalaJavaTime
  val scalajsSecureRandom =
    ("org.scala-js" %% "scalajs-java-securerandom" % Versions.scalajsSecureRandom).cross(
      CrossVersion.for3Use2_13
    )

  val play = "org.playframework" %% "play" % Versions.play
  val playJson = "org.playframework" %% "play-json" % Versions.playJson
  val playTest = "org.playframework" %% "play-test" % Versions.play % Test
  val playTestProvided = "org.playframework" %% "play-test" % Versions.play
  val guice = "com.google.inject" % "guice" % Versions.guice % "test"

  val json4sCore = "org.json4s" %% "json4s-core" % Versions.json4s
  val json4sNative = "org.json4s" %% "json4s-native-core" % Versions.json4s
  val json4sJackson = "org.json4s" %% "json4s-jackson-core" % Versions.json4s

  val circeCore = "io.circe" %% "circe-core" % Versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
  val circeJawn = "io.circe" %% "circe-jawn" % Versions.circe
  val circeParse = "io.circe" %% "circe-parser" % Versions.circe

  val upickle = "com.lihaoyi" %% "upickle" % Versions.upickle

  val zioJson = "dev.zio" %% "zio-json" % Versions.zioJson

  val argonaut = "io.argonaut" %% "argonaut" % Versions.argonaut

  val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk18on" % Versions.bouncyCastle % Test
  val bouncyCastleTut = "org.bouncycastle" % "bcpkix-jdk18on" % Versions.bouncyCastle

  val munit = "org.scalameta" %% "munit" % Versions.munit % Test
  val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % Versions.munit % Test
}
