import scala.sys.process.*

import com.github.sbt.sbtghpages.GhpagesPlugin
import com.jsuereth.sbtpgp.PgpKeys.*
import sbt.Keys.*
import sbt.Tests.*
import sbt.{given, *}

val previousVersion = "9.4.0"
val buildVersion = "9.4.1"

val scala213 = "3.10.0"
val scala3 = "3.3.7"

Global / onChangedBuildSource := ReloadOnSourceChanges
versionScheme := Some("early-semver")

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

val baseSettings = Seq(
  organization := "com.github.jwt-scala",
  scalaVersion := scala213,
  crossScalaVersions := Seq(scala213, scala3),
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
    case Some((2, _)) => Seq("-Xsource:3-cross")
    case _            => Nil
  })
)

val commonJvmSettings = Seq(
  javacOptions ++= Seq("--release", "17"),
  scalacOptions += "-release:17"
)

val publishSettings = Seq(
  homepage := Some(uri("https://jwt-scala.github.io/jwt-scala/")),
  apiURL := Some(uri("https://jwt-scala.github.io/jwt-scala/api/")),
  Test / publishArtifact := false,
  licenses += ("Apache-2.0", uri("http://www.apache.org/licenses/LICENSE-2.0")),
  pomIncludeRepository := { _ => false },
  scmInfo := Some(
    ScmInfo(
      uri("https://github.com/jwt-scala/jwt-scala"),
      "scm:git@github.com:jwt-scala/jwt-scala.git"
    )
  ),
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
  ),
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishSignedConfiguration := publishSignedConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishLocalSignedConfiguration := publishLocalSignedConfiguration.value.withOverwrite(true)
)

val noPublishSettings = Seq(
  publish / skip := true,
  publish := Def.uncached { () },
  publishLocal := Def.uncached { () },
  publishArtifact := false
)

lazy val commonJsSettings = Seq(
  Test / fork := false
)

val commonNativeSettings = Seq(
  libraryDependencySchemes += "org.scala-native" %
    s"test-interface_native0.5_${scalaBinaryVersion.value}" % VersionScheme.EarlySemVer
)

// Normal published settings
val releaseSettings = baseSettings ++ publishSettings

// Local non-published projects
val localSettings = baseSettings ++ noPublishSettings

lazy val jwtScala = project
  .in(file("."))
  .settings(localSettings, commonJvmSettings)
  .settings(
    name := "jwt-scala"
  )
  .aggregate(
    json4sNative,
    json4sJackson,
    circe.jvm,
    circe.js,
    circe.native,
    upickle.jvm,
    upickle.js,
    upickle.native,
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
    upickle.jvm,
    upickle.js,
    upickle.native,
    zioJson,
    playFramework,
    argonaut
  )
  .settings(crossScalaVersions := List())

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
  .settings(name := "jwt-docs")
  .settings(localSettings, commonJvmSettings)
  .settings(
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
      argonaut,
      zioJson
    ),
    baseSettings,
    publishArtifact := false,
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
  .settings(releaseSettings)
  .jvmSettings(commonJvmSettings)
  .nativeSettings(commonNativeSettings)
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
  .jvmSettings(commonJvmSettings)
  .nativeSettings(commonNativeSettings)
  .settings(
    name := "jwt-json-common"
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(Test / fork := false)
  .aggregate(core)
  .dependsOn(core % "compile->compile;test->test")

lazy val playJson = project
  .in(file("json/play-json"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-play-json",
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val circe = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("json/circe"))
  .settings(releaseSettings)
  .jvmSettings(commonJvmSettings)
  .nativeSettings(commonNativeSettings)
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

lazy val upickle = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("json/upickle"))
  .settings(releaseSettings)
  .jvmSettings(commonJvmSettings)
  .nativeSettings(commonNativeSettings)
  .settings(
    name := "jwt-upickle",
    libraryDependencies ++= Seq(Libs.upickle.value)
  )
  .jsSettings(commonJsSettings)
  .nativeSettings(Test / fork := false)
  .aggregate(jsonCommon)
  .dependsOn(jsonCommon % "compile->compile;test->test")

lazy val zioJson = project
  .in(file("json/zio-json"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-zio-json",
    libraryDependencies ++= Seq(Libs.zioJson)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sCommon = project
  .in(file("json/json4s-common"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-json4s-common",
    libraryDependencies ++= Seq(Libs.json4sCore)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

lazy val json4sNative = project
  .in(file("json/json4s-native"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-json4s-native",
    libraryDependencies ++= Seq(Libs.json4sNative)
  )
  .aggregate(json4sCommon)
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val json4sJackson = project
  .in(file("json/json4s-jackson"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-json4s-jackson",
    libraryDependencies ++= Seq(Libs.json4sJackson)
  )
  .aggregate(json4sCommon)
  .dependsOn(json4sCommon % "compile->compile;test->test")

lazy val argonaut = project
  .in(file("json/argonaut"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-argonaut",
    libraryDependencies ++= Seq(Libs.argonaut)
  )
  .aggregate(jsonCommon.jvm)
  .dependsOn(jsonCommon.jvm % "compile->compile;test->test")

def groupPlayTest(tests: Seq[TestDefinition]) = tests.map { t =>
  val options = ForkOptions()
  Group(t.name, Seq(t), SubProcess(options))
}

lazy val playFramework = project
  .in(file("play"))
  .settings(releaseSettings, commonJvmSettings)
  .settings(
    name := "jwt-play",
    libraryDependencies ++= Seq(Libs.play, Libs.playTest, Libs.guice),
    Test / testGrouping := Def.uncached {
      groupPlayTest((Test / definedTests).value)
    }
  )
  .aggregate(playJson)
  .dependsOn(playJson % "compile->compile;test->test")
