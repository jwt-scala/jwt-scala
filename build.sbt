import Dependencies._
import com.jsuereth.sbtpgp.PgpKeys._
import play.sbt.Play.autoImport._
import microsites._
import sbt.Keys._
import sbt.Tests._
import sbt._

import scala.sys.process._

val previousVersion = "7.1.1"
val buildVersion = "7.1.2"

ThisBuild / versionScheme := Some("early-semver")

val projects = Seq(
  "coreProject",
  "playJsonProject",
  "json4sNativeProject",
  "json4sJacksonProject",
  "sprayJsonProject",
  "circeProject",
  "upickleProject",
  "argonautProject",
  "playProject"
)

addCommandAlias("publish-doc", "docs/makeMicrosite; docs/publishMicrosite")

addCommandAlias("testAll", projects.map(p => p + "/test").mkString(";", ";", ""))

addCommandAlias("format", "scalafmt; test:scalafmt")

addCommandAlias("formatCheck", "scalafmtCheck; test:scalafmtCheck")

// ";+coreProject/publishSigned"
// ";+playJsonProject/publishSigned"
// ";+json4sNativeProject/publishSigned"
// ";+json4sJacksonProject/publishSigned"
// ";+sprayJsonProject/publishSigned"
// ";+circeProject/publishSigned"
// ";+upickleProject/publishSigned"
// ";+argonautProject/publishSigned"
// ";+playProject/publishSigned"
addCommandAlias("publishAll", projects.map(p => "+" + p + "/publishSigned").mkString(";", ";", ""))

addCommandAlias(
  "releaseAll",
  ";bumpScript;publish-doc;publishAll;sonatypeRelease;pushScript"
)

lazy val scaladocScript = taskKey[Unit]("Generate scaladoc and copy it to docs site")
scaladocScript := {
  "./scripts/scaladoc.sh " + buildVersion !
}

lazy val bumpScript = taskKey[Unit]("Bump the new version all around")
bumpScript := {
  "./scripts/bump.sh " + previousVersion + " " + buildVersion !
}

lazy val pushScript = taskKey[Unit]("Push to GitHub")
pushScript := {
  "./scripts/pu.sh " + buildVersion !
}

lazy val cleanScript = taskKey[Unit]("Clean tmp files")
cleanScript := {
  "./scripts/clean.sh" !
}

lazy val docsMappingsAPIDir: SettingKey[String] =
  settingKey[String]("Name of subdirectory in site target directory for api docs")

val scala212 = "2.12.13"
val scala213 = "2.13.5"
val scala3 = "3.0.0-RC1"

val crossVersionAll = Seq(scala212, scala213, scala3)
val crossVersion2Only = Seq(scala212, scala213)

val baseSettings = Seq(
  organization := "com.github.jwt-scala",
  version := buildVersion,
  ThisBuild / scalaVersion := scala213,
  crossScalaVersions := crossVersionAll,
  crossVersion := CrossVersion.binary,
  autoAPIMappings := true,
  libraryDependencies ++= Seq(Libs.scalatest, Libs.scalacheck),
  Test / aggregate := false,
  Test / fork := true,
  Test / parallelExecution := false,
  Compile / doc / scalacOptions ~= (_ filterNot (_ == "-Xfatal-warnings")),
  Compile / doc / scalacOptions ++= Seq(
    "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
  ),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

val publishSettings = Seq(
  homepage := Some(url("https://jwt-scala.github.io/jwt-scala/")),
  apiURL := Some(url("https://jwt-scala.github.io/jwt-scala/api/")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
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
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

// Normal published settings
val releaseSettings = baseSettings ++ publishSettings

// Local non-published projects
val localSettings = baseSettings ++ noPublishSettings

val docSettings = Seq(
  micrositeName := "JWT Scala",
  micrositeDescription := "JWT Scala",
  micrositeAuthor := "JWT Scala contributors",
  micrositeFooterText := Some(
    """
      |<p>© 2020 <a href="https://github.com/jwt-scala/jwt-scala">The JWT Scala Maintainers</a></p>
      |<p style="font-size: 80%; margin-top: 10px">Website built with <a href="https://47deg.github.io/sbt-microsites/">sbt-microsites © 2020 47 Degrees</a></p>
      |""".stripMargin
  ),
  micrositeHomepage := "https://jwt-scala.github.io/jwt-scala/",
  micrositeBaseUrl := "jwt-scala",
  micrositeDocumentationUrl := "/jwt-scala/api/index.html",
  micrositeGitterChannel := false,
  micrositeDocumentationLabelDescription := "API Documentation",
  micrositeExtraMdFilesOutput := resourceManaged.value / "main" / "jekyll",
  micrositeExtraMdFiles := Map(
    file("README.md") -> ExtraMdFileConfig(
      "index.md",
      "home",
      Map("title" -> "Home", "section" -> "home", "position" -> "0")
    )
  ),
  micrositeGithubRepo := "jwt-scala",
  micrositeSearchEnabled := false,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
    coreProject,
    playJsonProject,
    playProject,
    json4sNativeProject,
    sprayJsonProject,
    circeProject,
    upickleProject,
    argonautProject
  ),
  ScalaUnidoc / docsMappingsAPIDir := "api",
  addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, ScalaUnidoc / docsMappingsAPIDir),
  autoAPIMappings := true,
  ghpagesNoJekyll := false,
  ScalaUnidoc / unidoc / scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-groups",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-diagrams"
  ),
  scalacOptions ~= (_.filterNot(
    Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Ywarn-dead-code", "-Xfatal-warnings")
  )),
  git.remoteRepo := "git@github.com:jwt-scala/jwt-scala.git",
  makeSite / includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.yml" | "*.md" | "*.svg",
  Jekyll / includeFilter := (makeSite / includeFilter).value,
  mdocIn := (LocalRootProject / baseDirectory).value / "docs" / "src" / "main" / "mdoc",
  mdocExtraArguments := Seq("--no-link-hygiene")
)

lazy val jwtScala = project
  .in(file("."))
  .settings(localSettings)
  .settings(
    name := "jwt-scala"
  )
  .aggregate(
    json4sNativeProject,
    json4sJacksonProject,
    sprayJsonProject,
    circeProject,
    upickleProject,
    playProject,
    argonautProject
  )
  .dependsOn(
    json4sNativeProject,
    json4sJacksonProject,
    sprayJsonProject,
    circeProject,
    upickleProject,
    playProject,
    argonautProject
  )
  .settings(crossScalaVersions := List())

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(PreprocessPlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(name := "jwt-docs")
  .settings(localSettings)
  .settings(docSettings)
  .settings(
    libraryDependencies ++= Seq(
      Libs.bouncyCastleTut,
      Libs.playJson,
      Libs.play,
      Libs.playTestProvided,
      Libs.json4sNative,
      Libs.sprayJson,
      Libs.circeCore,
      Libs.circeGeneric,
      Libs.circeParse,
      Libs.upickle,
      Libs.argonaut
    )
  )
  .dependsOn(
    playProject,
    json4sNativeProject,
    sprayJsonProject,
    circeProject,
    upickleProject,
    argonautProject
  )

lazy val coreProject = project
  .in(file("core"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-core",
    libraryDependencies ++= Seq(Libs.bouncyCastle)
  )

lazy val jsonCommonProject = project
  .in(file("json/common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json-common"
  )
  .aggregate(coreProject)
  .dependsOn(coreProject % "compile->compile;test->test")

lazy val playJsonProject = project
  .in(file("json/play-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play-json",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val circeProject = project
  .in(file("json/circe"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-circe",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.circeCore, Libs.circeParse, Libs.circeGeneric % "test")
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val upickleProject = project
  .in(file("json/upickle"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-upickle",
    libraryDependencies ++= Seq(Libs.upickle)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val json4sCommonProject = project
  .in(file("json/json4s-common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-common",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.json4sCore)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val json4sNativeProject = project
  .in(file("json/json4s-native"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-native",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.json4sNative)
  )
  .aggregate(json4sCommonProject)
  .dependsOn(json4sCommonProject % "compile->compile;test->test")

lazy val json4sJacksonProject = project
  .in(file("json/json4s-jackson"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-jackson",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.json4sJackson)
  )
  .aggregate(json4sCommonProject)
  .dependsOn(json4sCommonProject % "compile->compile;test->test")

lazy val sprayJsonProject = project
  .in(file("json/spray-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-spray-json",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.sprayJson)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val argonautProject = project
  .in(file("json/argonaut"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-argonaut",
    libraryDependencies ++= Seq(Libs.argonaut)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

def groupPlayTest(tests: Seq[TestDefinition], files: Seq[File]) = tests.map { t =>
  val options = ForkOptions()
  Group(t.name, Seq(t), SubProcess(options))
}

lazy val playProject = project
  .in(file("play"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play",
    crossScalaVersions := crossVersion2Only,
    libraryDependencies ++= Seq(Libs.play, Libs.playTest, Libs.scalatestPlus, Libs.guice),
    Test / testGrouping := groupPlayTest(
      (Test / definedTests).value,
      (Test / dependencyClasspath).value.files
    )
  )
  .aggregate(playJsonProject)
  .dependsOn(playJsonProject % "compile->compile;test->test")
