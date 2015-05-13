import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import Dependencies._
//import bintray.Plugin.bintraySettings
//import bintray.Plugin.bintrayPublishSettings
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._

val previousVersion = "0.0.4"
val buildVersion = "0.0.5"

addCommandAlias("scaladoc", ";coreEdge/doc;playJsonEdge/doc;playEdge/doc;scaladocScript")
addCommandAlias("publish-doc", ";docs/makeSite;docs/ghpagesPushSite")
addCommandAlias("release", ";publishScript;scaladoc;publish-doc;+publish")

lazy val scaladocScript = taskKey[Unit]("Generate scaladoc and copy it to docs site")
scaladocScript := {
  "./scaladoc.sh "+buildVersion !
}

lazy val publishScript = taskKey[Unit]("Bump the new version all around and push it to GitHub")
publishScript := {
  "./publi.sh "+previousVersion+" "+buildVersion !
}

val commonSettings = Seq(
  organization := "pdi",
  version := buildVersion,
  scalaVersion := "2.11.6",
  autoAPIMappings := true,
  crossScalaVersions := Seq("2.10.5", "2.11.6"),
  crossVersion := CrossVersion.binary,
  resolvers ++= Seq(
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(Libs.bouncyCastle, Libs.scalatest, Libs.jmockit),
  scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation")
)

val publishSettings = bintraySettings ++ bintrayPublishSettings ++ Seq(
  homepage := Some(url("https://github.com/pauldijou/jwt-scala")),
  apiURL := Some(url("https://pauldijou.github.io/jwt-scala/api/")),
  publishMavenStyle := true,
  publishArtifact in packageDoc := false,
  publishArtifact in Test := false,
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

val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

val docSettings = Seq(
  site.addMappingsToSiteDir(tut, "_includes/tut"),
  ghpagesNoJekyll := false,
  siteMappings ++= Seq(
    file("README.md") -> "_includes/README.md"
  ),
  git.remoteRepo := "git@github.com:pauldijou/jwt-scala.git",
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md" | "*.scss"
)

lazy val jwtScala = project.in(file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "jwt-scala"
  )
  .aggregate(playEdge, playLegacy)
  .dependsOn(playEdge, playLegacy)

lazy val docs = project.in(file("docs"))
  .settings(name := "jwt-docs")
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(site.settings)
  .settings(ghpages.settings)
  .settings(tutSettings)
  .settings(docSettings)
  .settings(
    libraryDependencies ++= Seq(Libs.playJson, Libs.play, Libs.playTest)
  )
  .dependsOn(playEdge)

lazy val coreLegacy = project.in(file("core/legacy"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-core-legacy-impl",
    libraryDependencies ++= Seq(Libs.apacheCodec)
  )

lazy val coreEdge = project.in(file("core/edge"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-core-impl"
  )

lazy val coreCommonLegacy = project.in(file("core/common"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-core-legacy",
    target <<= target(_ / "legacy")
  )
  .aggregate(coreLegacy)
  .dependsOn(coreLegacy % "compile->compile;test->test")

lazy val coreCommonEdge = project.in(file("core/common"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-core",
    target <<= target(_ / "edge")
  )
  .aggregate(coreEdge)
  .dependsOn(coreEdge % "compile->compile;test->test")

lazy val playJsonLegacy = project.in(file("play-json"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-play-json-legacy",
    target <<= target(_ / "legacy"),
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(coreCommonLegacy)
  .dependsOn(coreCommonLegacy % "compile->compile;test->test")

lazy val playJsonEdge = project.in(file("play-json"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-play-json",
    target <<= target(_ / "edge"),
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(coreCommonEdge)
  .dependsOn(coreCommonEdge % "compile->compile;test->test")

lazy val playLegacy = project.in(file("play"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-play-legacy",
    target <<= target(_ / "legacy"),
    libraryDependencies ++= Seq(Libs.play),
    bintray.Keys.packageLabels in bintray.Keys.bintray ++= Seq("play", "play framework")
  )
  .aggregate(playJsonLegacy)
  .dependsOn(playJsonLegacy % "compile->compile;test->test")

lazy val playEdge = project.in(file("play"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "jwt-play",
    target <<= target(_ / "edge"),
    libraryDependencies ++= Seq(Libs.play),
    bintray.Keys.packageLabels in bintray.Keys.bintray ++= Seq("play", "play framework")
  )
  .aggregate(playJsonEdge)
  .dependsOn(playJsonEdge % "compile->compile;test->test")

lazy val examplePlayAngularProject = project.in(file("examples/play-angular"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "playAngular"
  )
  .enablePlugins(play.PlayScala)
  .aggregate(playLegacy)
  .dependsOn(playLegacy)
