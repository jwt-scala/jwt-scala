import scala.sys.process.*

import com.github.sbt.sbtghpages.GhpagesPlugin
import com.jsuereth.sbtpgp.PgpKeys.*
import sbt.Keys.*
import sbt.Tests.*
import sbt.{given, *}

val previousVersion = "9.4.0"
val buildVersion = "9.4.1"

val scala213 = "2.13.18"
val scala3 = "3.3.7"

Global / onChangedBuildSource := ReloadOnSourceChanges
versionScheme := Some("early-semver")

// Common / bare settings automatically applied to all projects in SBT 2
organization := "com.github.jwt-scala"
scalaVersion := scala213
crossScalaVersions := Seq(scala213, scala3)
autoAPIMappings := true
libraryDependencies ++= Seq(Libs.munit, Libs.munitScalacheck)
testFrameworks += new TestFramework("munit.Framework")
mimaFailOnNoPrevious := false
Test / aggregate := false
Test / fork := true
Test / parallelExecution := false
javacOptions ++= Seq("--release", "17")
scalacOptions += "-release:17"
Compile / doc / scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings"))
Compile / doc / scalacOptions ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)
scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, _)) => Seq("-Xsource:3-cross")
  case _            => Nil
})

// Publishing settings (common to published modules)
homepage := Some(uri("https://jwt-scala.github.io/jwt-scala/"))
apiURL := Some(uri("https://jwt-scala.github.io/jwt-scala/api/"))
Test / publishArtifact := false
licenses := Seq(License.Apache2)
pomIncludeRepository := { _ => false }
scmInfo := Some(
  ScmInfo(
    uri("https://github.com/jwt-scala/jwt-scala"),
    "scm:git@github.com:jwt-scala/jwt-scala.git"
  )
)
developers := List(
  Developer(
    id = "pdi",
    name = "Paul Dijou",
    email = "paul.dijou@gmail.com",
    url = uri("http://pauldijou.fr")
  ),
  Developer(
    id = "erwan",
    name = "Erwan Loisant",
    email = "erwan@loisant.com",
    url = uri("https://caffeinelab.net")
  )
)
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishSignedConfiguration := publishSignedConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishLocalSignedConfiguration := publishLocalSignedConfiguration.value.withOverwrite(true)

val projects = Seq(
  "playJson",
  "json4sCommon",
  "json4sNative",
  "json4sJackson",
  "zioJson",
  "argonaut",
  "playFramework"
)
val crossProjects = Seq(
  "core",
  "jsonCommon",
  "circe",
  "upickle"
)
val allProjects = crossProjects.flatMap(p => Seq(s"${p}JVM", s"${p}JS", s"${p}Native")) ++ projects

addCommandAlias("publish-doc", "docs/makeSite; docs/ghpagesPushSite")

addCommandAlias("testAll", allProjects.map(p => p + "/Test/testFull").mkString(";", ";", ""))

addCommandAlias("format", "all scalafmtAll scalafmtSbt")

addCommandAlias("formatCheck", "all scalafmtCheckAll scalafmtSbtCheck")

lazy val cleanScript = taskKey[Unit]("Clean tmp files")
LocalRootProject / cleanScript := Def.uncached {
  val exitCode = Process(Seq("sh", "scripts/clean.sh"), baseDirectory.value).!
  require(exitCode == 0, s"Cleaning temporary files failed with exit code $exitCode")
}

lazy val docsMappingsAPIDir: SettingKey[String] =
  settingKey[String]("Name of subdirectory in site target directory for api docs")

val noPublishSettings = Seq(
  publish / skip := true,
  publishArtifact := false
)

lazy val commonJsSettings = Seq(
  Test / fork := false
)

val commonNativeSettings = Seq(
  libraryDependencySchemes += "org.scala-native" %
    s"test-interface_native0.5_${scalaBinaryVersion.value}" % VersionScheme.EarlySemVer,
  Test / fork := false
)

lazy val jwtScala = project
  .in(file("."))
  .settings(noPublishSettings)
  .settings(
    name := "jwt-scala",
    crossScalaVersions := List()
  )
  .aggregate(
    core.jvm,
    core.js,
    core.native,
    jsonCommon.jvm,
    jsonCommon.js,
    jsonCommon.native,
    playJson,
    circe.jvm,
    circe.js,
    circe.native,
    upickle.jvm,
    upickle.js,
    upickle.native,
    zioJson,
    json4sCommon,
    json4sNative,
    json4sJackson,
    argonaut,
    playFramework
  )

lazy val docs = project
  .in(file("docs"))
  .enablePlugins(
    GhpagesPlugin,
    SitePreviewPlugin,
    SiteScaladocPlugin,
    ScalaUnidocPlugin,
    ParadoxSitePlugin,
    ParadoxMaterialThemePlugin
  )
  .settings(noPublishSettings)
  .settings(
    name := "jwt-docs",
    libraryDependencies ++= Seq("org.playframework" %% "play-test" % Versions.play),
    ScalaUnidoc / siteSubdirName := "api",
    addMappingsToSiteDir(
      ScalaUnidoc / packageDoc / mappings,
      ScalaUnidoc / siteSubdirName
    ),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      core.jvm,
      circe.jvm,
      json4sNative,
      upickle.jvm,
      zioJson,
      playJson,
      playFramework,
      argonaut
    ),
    Compile / paradoxMaterialTheme ~= (_.withRepository(
      uri("https://github.com/jwt-scala/jwt-scala")
    )),
    git.remoteRepo := "git@github.com:jwt-scala/jwt-scala.git",
    siteDirectory := (LocalRootProject / baseDirectory).value / "target" / "site",
    packageSite / artifactPath := fileConverter.value.toVirtualFile(
      ((LocalRootProject / baseDirectory).value / "target" / "artifact.zip").toPath
    )
  )
  .dependsOn(playFramework, json4sNative, circe.jvm, upickle.jvm, zioJson, argonaut)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(
    name := "jwt-core",
    libraryDependencies += Libs.bouncyCastle
  )
  .jsSettings(
    commonJsSettings,
    libraryDependencies ++= Seq(
      Libs.scalaJavaTime,
      Libs.scalajsSecureRandom
    )
  )
  .nativeSettings(
    commonNativeSettings,
    libraryDependencies += Libs.scalaJavaTime
  )

lazy val jsonCommon = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("json/common"))
  .settings(name := "jwt-json-common")
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .dependsOn(core % "compile->compile;test->test")

lazy val playJson = project
  .in(file("json/play-json"))
  .settings(
    name := "jwt-play-json",
    libraryDependencies += Libs.playJson
  )
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("json/circe"))
  .settings(
    name := "jwt-circe",
    libraryDependencies ++= Seq(
      Libs.circeCore,
      Libs.circeJawn,
      Libs.circeParse,
      Libs.circeGeneric % "test"
    )
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .dependsOn(jsonCommon % "compile->compile;test->test")

lazy val upickle = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("json/upickle"))
  .settings(
    name := "jwt-upickle",
    libraryDependencies += Libs.upickle
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(commonNativeSettings)
  .dependsOn(jsonCommon % "compile->compile;test->test")

lazy val zioJson = project
  .in(file("json/zio-json"))
  .settings(
    name := "jwt-zio-json",
    libraryDependencies += Libs.zioJson
  )
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sCommon = project
  .in(file("json/json4s-common"))
  .settings(
    name := "jwt-json4s-common",
    libraryDependencies += Libs.json4sCore
  )
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sNative = project
  .in(file("json/json4s-native"))
  .settings(
    name := "jwt-json4s-native",
    libraryDependencies += Libs.json4sNative
  )
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val json4sJackson = project
  .in(file("json/json4s-jackson"))
  .settings(
    name := "jwt-json4s-jackson",
    libraryDependencies += Libs.json4sJackson
  )
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val argonaut = project
  .in(file("json/argonaut"))
  .settings(
    name := "jwt-argonaut",
    libraryDependencies += Libs.argonaut
  )
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

def groupPlayTest(tests: Seq[TestDefinition]) = tests.map { t =>
  val options = ForkOptions()
  Group(t.name, Seq(t), SubProcess(options))
}

lazy val playFramework = project
  .in(file("play"))
  .settings(
    name := "jwt-play",
    libraryDependencies ++= Seq(Libs.play, Libs.playTest, Libs.guice),
    Test / testGrouping := Def.uncached {
      groupPlayTest((Test / definedTests).value)
    }
  )
  .dependsOn(playJson % "compile->compile;test->test")
