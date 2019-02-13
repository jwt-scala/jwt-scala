import sbt._
import Keys._
import Tests._
import play.sbt.Play.autoImport._
import PlayKeys._
import Dependencies._
import scala.sys.process._
import com.typesafe.sbt.pgp.PgpKeys._

val previousVersion = "1.1.0"
val buildVersion = "2.0.0"

val projects = Seq("coreProject", "playJsonProject", "json4sNativeProject", "json4sJacksonProject", "sprayJsonProject", "circeProject", "upickleProject", "argonautProject", "playProject")

addCommandAlias("scaladoc", ";coreProject/doc;playJsonProject/doc;json4sNativeProject/doc;sprayJsonProject/doc;circeProject/doc;upickleProject/doc;argonautProject/doc;playProject/doc;scaladocScript;cleanScript")

addCommandAlias("publish-doc", ";docs/makeSite;docs/tut;docs/ghpagesPushSite")

addCommandAlias("testAll", projects.map(p => p + "/test").mkString(";", ";", ""))
addCommandAlias("publishAll", projects.map(p => "+" + p + "/publishSigned").mkString(";", ";", ""))

addCommandAlias("releaseAll", ";bumpScript;scaladoc;publish-doc;publishAll;sonatypeRelease;pushScript")

lazy val scaladocScript = taskKey[Unit]("Generate scaladoc and copy it to docs site")
scaladocScript := {
  "./scripts/scaladoc.sh "+buildVersion !
}

lazy val bumpScript = taskKey[Unit]("Bump the new version all around")
bumpScript := {
  "./scripts/bump.sh "+previousVersion+" "+buildVersion !
}

lazy val pushScript = taskKey[Unit]("Push to GitHub")
pushScript := {
  "./scripts/pu.sh "+buildVersion !
}

lazy val cleanScript = taskKey[Unit]("Clean tmp files")
cleanScript := {
  "./scripts/clean.sh" !
}

def jmockitPath(f: Seq[File]) = f.filter(_.name.endsWith("jmockit-1.24.jar")).head

val baseSettings = Seq(
  organization := "com.pauldijou",
  version := buildVersion,
  scalaVersion in ThisBuild := "2.12.7",
  crossScalaVersions := Seq("2.12.7", "2.11.12"),
  crossVersion := CrossVersion.binary,
  autoAPIMappings := true,
  resolvers ++= Seq(
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(Libs.scalatest, Libs.jmockit),
  Test / aggregate := false,
  Test / fork := true,
  Test / parallelExecution := false,
  scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation"),
  javaOptions in Test += s"-javaagent:${jmockitPath((dependencyClasspath in Test).value.files)}"
)

val publishSettings = Seq(
  homepage := Some(url("http://pauldijou.fr/jwt-scala/")),
  organizationHomepage := Some(url("http://pauldijou.github.io/")),
  apiURL := Some(url("http://pauldijou.fr/jwt-scala/api/")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/pauldijou/jwt-scala"),
      "scm:git@github.com:pauldijou/jwt-scala.git"
    )
  ),
  developers := List(
    Developer(id="pdi", name="Paul Dijou", email="paul.dijou@gmail.com", url=url("http://pauldijou.fr"))
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
  git.remoteRepo := "git@github.com:pauldijou/jwt-scala.git",
  sourceDirectory in Preprocess := tutTargetDirectory.value,
  ghpagesNoJekyll := false,
  git.remoteRepo := "git@github.com:pauldijou/jwt-scala.git",
  mappings in makeSite ++= Seq(
    file("README.md") -> "_includes/README.md"
  ),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md" | "*.scss",
  fork in tut := true,
)

lazy val jwtScala = project.in(file("."))
  .settings(localSettings)
  .settings(
    name := "jwt-scala"
  )
  .aggregate(json4sNativeProject, json4sJacksonProject, sprayJsonProject, circeProject, upickleProject, playProject, argonautProject)
  .dependsOn(json4sNativeProject, json4sJacksonProject, sprayJsonProject, circeProject, upickleProject, playProject, argonautProject)

lazy val docs = project.in(file("docs"))
  .enablePlugins(PreprocessPlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(TutPlugin)
  .settings(name := "jwt-docs")
  .settings(localSettings)
  .settings(docSettings)
  .settings(
    libraryDependencies ++= Seq(
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
  .dependsOn(playProject, json4sNativeProject, sprayJsonProject, circeProject, upickleProject, argonautProject)

lazy val coreProject = project.in(file("core"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-core",
    libraryDependencies ++= Seq(Libs.bouncyCastle)
  )

lazy val jsonCommonProject = project.in(file("json/common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json-common",
  )
  .aggregate(coreProject)
  .dependsOn(coreProject % "compile->compile;test->test")

lazy val playJsonProject = project.in(file("json/play-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play-json",
    libraryDependencies ++= Seq(Libs.playJson)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val circeProject = project.in(file("json/circe"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-circe",
    libraryDependencies ++= Seq(Libs.circeCore, Libs.circeGeneric, Libs.circeParse)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val upickleProject = project.in(file("json/upickle"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-upickle",
    libraryDependencies ++= Seq(Libs.upickle)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val json4sCommonProject = project.in(file("json/json4s-common"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-common",
    libraryDependencies ++= Seq(Libs.json4sCore)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val json4sNativeProject = project.in(file("json/json4s-native"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-native",
    libraryDependencies ++= Seq(Libs.json4sNative)
  )
  .aggregate(json4sCommonProject)
  .dependsOn(json4sCommonProject % "compile->compile;test->test")

lazy val json4sJacksonProject = project.in(file("json/json4s-jackson"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-json4s-jackson",
    libraryDependencies ++= Seq(Libs.json4sJackson)
  )
  .aggregate(json4sCommonProject)
  .dependsOn(json4sCommonProject % "compile->compile;test->test")

lazy val sprayJsonProject = project.in(file("json/spray-json"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-spray-json",
    libraryDependencies ++= Seq(Libs.sprayJson)
  )
  .aggregate(jsonCommonProject)
  .dependsOn(jsonCommonProject % "compile->compile;test->test")

lazy val argonautProject = project.in(file("json/argonaut"))
    .settings(releaseSettings)
    .settings(
      name := "jwt-argonaut",
      libraryDependencies ++= Seq(Libs.argonaut)
    )
    .aggregate(jsonCommonProject)
    .dependsOn(jsonCommonProject % "compile->compile;test->test")

def groupPlayTest(tests: Seq[TestDefinition], files: Seq[File]) = tests.map { t =>
  val options = ForkOptions().withRunJVMOptions(Vector(s"-javaagent:${jmockitPath(files)}"))
  new Group(t.name, Seq(t), new SubProcess(options))
}

lazy val playProject = project.in(file("play"))
  .settings(releaseSettings)
  .settings(
    name := "jwt-play",
    libraryDependencies ++= Seq(Libs.play, Libs.playTest, Libs.scalatestPlus, Libs.guice),
    testGrouping in Test := groupPlayTest((definedTests in Test).value, (dependencyClasspath in Test).value.files)
  )
  .aggregate(playJsonProject)
  .dependsOn(playJsonProject % "compile->compile;test->test")

lazy val examplePlayAngularProject = project.in(file("examples/play-angular"))
  .settings(localSettings)
  .settings(
    name := "playAngular",
    libraryDependencies ++= Seq(guice),
    routesGenerator := play.sbt.routes.RoutesKeys.InjectedRoutesGenerator
  )
  .enablePlugins(PlayScala)
  .aggregate(playProject)
  .dependsOn(playProject)
