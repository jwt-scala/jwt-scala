import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import Dependencies._
import bintray.Plugin.bintraySettings
import bintray.Plugin.bintrayPublishSettings

object ProjectBuild extends Build {
  val CommonSettings = Seq(
    organization := "pdi",
    version := "0.0.2",
    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.10.5", "2.11.6"),
    publishArtifact := false,
    resolvers ++= Seq(
      "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= Seq(Libs.scalatest, Libs.jmockit)
  )

  val PublishSettings = bintraySettings ++ bintrayPublishSettings ++ Seq(
    publishMavenStyle := true,
    publishArtifact := true,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    bintray.Keys.packageLabels in bintray.Keys.bintray := Seq("jwt", "scala", "authentication", "json", "web", "token"),
    pomExtra := (
      <scm>
        <url>git@github.com:pauldijou/jwt-scala.git</url>
        <connection>scm:git:git@github.com:pauldijou/jwt-scala.git</connection>
      </scm>
      <developers>
        <developer>
          <id>pdi</id>
          <name>Paul Dijou</name>
          <url>http://pauldijou.fr</url>
        </developer>
      </developers>)
  )

  lazy val coreLegacy = Project("coreLegacy", file("core/legacy"))
    .settings(CommonSettings: _*)
    .settings(
      name := "core-legacy",
      libraryDependencies ++= Seq(Libs.apacheCodec)
    )

  lazy val coreEdge = Project("coreEdge", file("core/edge"))
    .settings(CommonSettings: _*)
    .settings(
      name := "core-edge"
    )

  lazy val coreCommonLegacy = Project("coreCommonLegacy", file("core/common"))
    .settings(CommonSettings: _*)
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-core-legacy",
      target <<= target(_ / "legacy")
    )
    .aggregate(coreLegacy)
    .dependsOn(coreLegacy % "compile->compile;test->test")

  lazy val coreCommonEdge = Project("coreCommonEdge", file("core/common"))
    .settings(CommonSettings: _*)
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-core",
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
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-play-json-legacy",
      target <<= target(_ / "legacy"),
      libraryDependencies ++= Seq(Libs.playJson)
    )
    .aggregate(coreCommonLegacy)
    .dependsOn(coreCommonLegacy % "compile->compile;test->test")

  lazy val playJsonEdge = Project("playJsonEdge", file("play-json"))
    .settings(CommonSettings: _*)
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-play-json",
      target <<= target(_ / "edge"),
      libraryDependencies ++= Seq(Libs.playJson)
    )
    .aggregate(coreCommonEdge)
    .dependsOn(coreCommonEdge % "compile->compile;test->test")

  lazy val playLegacy = Project("playLegacy", file("play"))
    .settings(CommonSettings: _*)
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-play-legacy",
      target <<= target(_ / "legacy"),
      libraryDependencies ++= Seq(Libs.play),
      bintray.Keys.packageLabels in bintray.Keys.bintray ++= Seq("play", "play framework")
    )
    .aggregate(playJsonLegacy)
    .dependsOn(playJsonLegacy % "compile->compile;test->test")

  lazy val playEdge = Project("playEdge", file("play"))
    .settings(CommonSettings: _*)
    .settings(PublishSettings: _*)
    .settings(
      name := "jwt-play",
      target <<= target(_ / "edge"),
      libraryDependencies ++= Seq(Libs.play),
      bintray.Keys.packageLabels in bintray.Keys.bintray ++= Seq("play", "play framework")
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
