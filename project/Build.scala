import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import Dependencies._

object ProjectBuild extends Build {
  val CommonSettings = Seq(
    organization := "pdi",
    version := "0.1.0",
    scalaVersion := "2.11.2",
    /*sourcesInBase := true,*/
    resolvers := Seq(
      "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= Seq(Libs.scalatest, Libs.jmockit)
  )

  lazy val coreLegacy = Project("coreLegacy", file("core/legacy"))
    .settings(CommonSettings: _*)
    .settings(
      name := "coreLegacy",
      libraryDependencies ++= Seq(Libs.apacheCodec)
    )

  lazy val coreEdge = Project("coreEdge", file("core/edge"))
    .settings(CommonSettings: _*)
    .settings(
      name := "coreEdge"
    )

  lazy val coreCommonLegacy = Project("coreCommonLegacy", file("core/common"))
    .settings(CommonSettings: _*)
    .settings(
      name := "coreCommonLegacy",
      target <<= target(_ / "legacy")
    )
    .aggregate(coreLegacy)
    .dependsOn(coreLegacy % "compile->compile;test->test")

  lazy val coreCommonEdge = Project("coreCommonEdge", file("core/common"))
    .settings(CommonSettings: _*)
    .settings(
      name := "coreCommonEdge",
      target <<= target(_ / "edge")
    )
    .aggregate(coreEdge)
    .dependsOn(coreEdge % "compile->compile;test->test")

  /*lazy val json4sNativeProject = Project("json4s", file("json4s"))
    .settings(CommonSettings: _*)
    .settings(
      name := "json4s",
      libraryDependencies ++= Seq(Libs.json4sNative)
    )
    .aggregate(coreProject)
    .dependsOn(coreProject % "compile->compile;test->test")

  lazy val json4sJacksonProject = Project("json4s", file("json4s"))
    .settings(CommonSettings: _*)
    .settings(
      name := "json4s",
      libraryDependencies ++= Seq(Libs.json4sJackson)
    )
    .aggregate(coreProject)
    .dependsOn(coreProject % "compile->compile;test->test")*/

  lazy val playJsonLegacy = Project("playJsonLegacy", file("play-json"))
    .settings(CommonSettings: _*)
    .settings(
      name := "playJsonLegacy",
      target <<= target(_ / "legacy"),
      libraryDependencies ++= Seq(Libs.playJson)
    )
    .aggregate(coreCommonLegacy)
    .dependsOn(coreCommonLegacy % "compile->compile;test->test")

  lazy val playJsonEdge = Project("playJsonEdge", file("play-json"))
    .settings(CommonSettings: _*)
    .settings(
      name := "playJsonEdge",
      target <<= target(_ / "edge"),
      libraryDependencies ++= Seq(Libs.playJson)
    )
    .aggregate(coreCommonEdge)
    .dependsOn(coreCommonEdge % "compile->compile;test->test")

  lazy val playLegacy = Project("playLegacy", file("play"))
    .settings(CommonSettings: _*)
    .settings(
      name := "playLegacy",
      target <<= target(_ / "legacy"),
      libraryDependencies ++= Seq(Libs.play)
    )
    .aggregate(playJsonLegacy)
    .dependsOn(playJsonLegacy % "compile->compile;test->test")

  lazy val playEdge = Project("playEdge", file("play"))
    .settings(CommonSettings: _*)
    .settings(
      name := "playEdge",
      target <<= target(_ / "edge"),
      libraryDependencies ++= Seq(Libs.play)
    )
    .aggregate(playJsonEdge)
    .dependsOn(playJsonEdge % "compile->compile;test->test")

  lazy val examplePlayAngularProject = Project("playAngular", file("examples/play-angular"))
    .settings(CommonSettings: _*)
    .settings(
      name := "playAngular"
    )
    .enablePlugins(play.PlayScala)
    .aggregate(playLegacy)
    .dependsOn(playLegacy)
}
