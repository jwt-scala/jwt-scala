import scala.io.Source
import scala.sys.process._

import com.jsuereth.sbtpgp.PgpKeys._
import sbt.Keys._
import sbt.Tests._
import sbt._

val previousVersion = "9.4.0"
val buildVersion = "9.4.1"

val scala212 = "2.12.15"
val scala213 = "2.13.11"
val scala3 = "3.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / versionScheme := Some("early-semver")

val projects = Seq(
  "playJson",
  "json4sNative",
  "json4sJackson",
  "upickle",
  "zioJson",
  "argonaut",
  "playFramework"
)
val crossProjects = Seq(
  "core",
  "circe"
)
val allProjects = crossProjects.flatMap(p => Seq(s"${p}JVM", s"${p}JS", s"${p}Native")) ++ projects

addCommandAlias("publish-doc", "docs/makeMicrosite; docs/publishMicrosite")

addCommandAlias("testAll", allProjects.map(p => p + "/test").mkString(";", ";", ""))

addCommandAlias("format", "all scalafmtAll scalafmtSbt")

addCommandAlias("formatCheck", "all scalafmtCheckAll scalafmtSbtCheck")

lazy val scaladocScript = taskKey[Unit]("Generate scaladoc and copy it to docs site")
scaladocScript := {
  "./scripts/scaladoc.sh " + buildVersion !
}

lazy val cleanScript = taskKey[Unit]("Clean tmp files")
cleanScript := {
  "./scripts/clean.sh" !
}

lazy val docsMappingsAPIDir: SettingKey[String] =
  settingKey[String]("Name of subdirectory in site target directory for api docs")

val crossVersionAll = Seq(scala212, scala213, scala3)
val crossVersion2Only = Seq(scala212, scala213)

val baseSettings = Seq(
  organization := "com.github.jwt-scala",
  ThisBuild / scalaVersion := scala213,
  crossScalaVersions := crossVersionAll,
  autoAPIMappings := true,
  libraryDependencies ++= Seq(Libs.munit.value, Libs.munitScalacheck.value),
  testFrameworks += new TestFramework("munit.Framework"),
  mimaFailOnNoPrevious := false,
  Test / aggregate := false,
  Test / fork := true,
  Test / parallelExecution := false,
  Compile / doc / scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
  Compile / doc / scalacOptions ++= Seq(
    "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
  ),
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("-Xsource:3")
    case _            => Nil
  }),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

val publishSettings = Seq(
  ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
  homepage := Some(url("https://jwt-scala.github.io/jwt-scala/")),
  apiURL := Some(url("https://jwt-scala.github.io/jwt-scala/api/")),
  Test / publishArtifact := false,
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  pomIncludeRepository := { _ => false },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/jwt-scala/jwt-scala"),
      "scm:git@github.com:jwt-scala/jwt-scala.git"
    )
  ),
  developers := List(
    Developer(
      id = "pdi",
      name = "Paul Dijou",
      email = "paul.dijou@gmail.com",
      url = url("http://pauldijou.fr")
    ),
    Developer(
      id = "erwan",
      name = "Erwan Loisant",
      email = "erwan@loisant.com",
      url = url("https://caffeinelab.net")
    )
  ),
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishSignedConfiguration := publishSignedConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishLocalSignedConfiguration := publishLocalSignedConfiguration.value.withOverwrite(true)
)

val noPublishSettings = Seq(
  publish := (()),
  publishLocal := (()),
  publishArtifact := false
)

lazy val commonJsSettings = Seq(
  Test / fork := false
)

// Normal published settings
val releaseSettings = baseSettings ++ publishSettings

// Local non-published projects
val localSettings = baseSettings ++ noPublishSettings

lazy val jwtScala = project
  .in(file("."))
  .settings(localSettings)
  .settings(
    name := "jwt-scala"
  )
  .aggregate(
    json4sNative,
    json4sJackson,
    circe.jvm,
    circe.js,
    circe.native,
    upickle,
    zioJson,
    playFramework,
    argonaut
  )
  .dependsOn(
    json4sNative,
    json4sJackson,
    circe.jvm,
    circe.js,
    circe.native,
    upickle,
    zioJson,
    playFramework,
    argonaut
  )
  .settings(crossScalaVersions := List())

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(
    SitePreviewPlugin,
    SiteScaladocPlugin,
    ScalaUnidocPlugin,
    ParadoxSitePlugin,
    ParadoxMaterialThemePlugin
  )
  .settings(name := "jwt-docs")
  .settings(localSettings)
  .settings(
    ScalaUnidoc / siteSubdirName := "api",
    addMappingsToSiteDir(
      ScalaUnidoc / packageDoc / mappings,
      ScalaUnidoc / siteSubdirName
    ),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      core.jvm,
      circe.jvm,
      json4sNative,
      json4sJackson,
      upickle,
      zioJson,
      playJson,
      playFramework,
      argonaut,
      zioJson
    ),
    baseSettings,
    publishArtifact := false,
    Compile / paradoxMaterialTheme ~= (_.withRepository(
      uri("https://github.com/jwt-scala/jwt-scala")
    )),
    packageSite / artifactPath := new java.io.File("target/artifact.zip")
  )
  .dependsOn(playFramework, json4sNative, circe.jvm, upickle, zioJson, argonaut)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(releaseSettings)
  .settings(name := "jwt-core", libraryDependencies ++= Seq(Libs.bouncyCastle))
  .jsSettings(commonJsSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      Libs.scalaJavaTime.value,
      Libs.scalajsSecureRandom.value
    )
  )
  .nativeSettings(
    libraryDependencies ++= Seq(
      Libs.scalaJavaTime.value
    ),
    Test / fork := false
  )

lazy val jsonCommon = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("json/common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json-common"
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(Test / fork := false)
  .aggregate(core)
  .dependsOn(core % "compile->compile;test->test")

lazy val playJson = project
  .in(file("json/play-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play-json",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("json/circe"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-circe",
    libraryDependencies ++= Seq(
      Libs.circeCore.value,
      Libs.circeJawn.value,
      Libs.circeParse.value,
      Libs.circeGeneric.value % "test"
    )
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(Test / fork := false)
  .aggregate(jsonCommon)
  .dependsOn(jsonCommon % "compile->compile;test->test")

lazy val upickle = project
  .in(file("json/upickle"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-upickle",
    libraryDependencies ++= Seq(Libs.upickle)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val zioJson = project
  .in(file("json/zio-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-zio-json",
    libraryDependencies ++= Seq(Libs.zioJson)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sCommon = project
  .in(file("json/json4s-common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-common",
    libraryDependencies ++= Seq(Libs.json4sCore)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sNative = project
  .in(file("json/json4s-native"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-native",
    libraryDependencies ++= Seq(Libs.json4sNative)
  )
  .aggregate(json4sCommon)
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val json4sJackson = project
  .in(file("json/json4s-jackson"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-jackson",
    libraryDependencies ++= Seq(Libs.json4sJackson)
  )
  .aggregate(json4sCommon)
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val argonaut = project
  .in(file("json/argonaut"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-argonaut",
    libraryDependencies ++= Seq(Libs.argonaut)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

def groupPlayTest(tests: Seq[TestDefinition], files: Seq[File]) = tests.map { t =>
  val options = ForkOptions()
  Group(t.name, Seq(t), SubProcess(options))
}

lazy val playFramework = project
  .in(file("play"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.play, Libs.playTest, Libs.guice),
    Test / testGrouping := groupPlayTest(
      (Test / definedTests).value,
      (Test / dependencyClasspath).value.files
    )
  )
  .aggregate(playJson)
  .dependsOn(playJson % "compile->compile;test->test")
