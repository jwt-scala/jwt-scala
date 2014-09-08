import sbt._
import Keys._
import Dependencies._

object ProjectBuild extends Build {
  lazy val Java8 = config("java8").extend( Compile )
  lazy val JavaLegacy = config("javaLegacy").extend( Compile )

  val CommonSettings = Defaults.defaultSettings ++ Seq(
    organization := "pdi",
    version := "0.1.0",
    scalaVersion := "2.11.2",
    sourcesInBase := false,
    // What I would like to do...
    // unmanagedSourceDirectories in Java8 += baseDirectory.value / "src/main/scala-java-8",
    // unmanagedSourceDirectories in JavaLegacy += baseDirectory.value / "src/main/scala-java-legacy",
    // But it failed, so I ended up with that:
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src/main/scala-java-8",
    libraryDependencies ++= Seq(scalatest)
  )

  lazy val coreProject = Project("core", file("scala-jwt-core"))
    .configs(Java8, JavaLegacy)
    .settings(CommonSettings: _*)
    .settings( inConfig(Java8)(Defaults.configTasks): _* )
    .settings( inConfig(JavaLegacy)(Defaults.configTasks): _* )
    .settings(
      name := "core"
    )

  lazy val json4sNativeProject = Project("json4s", file("scala-jwt-json4s"))
    .settings(CommonSettings: _*)
    .settings(
      name := "json4s",
      libraryDependencies ++= Seq(json4sNative)
    )
    .aggregate(coreProject)
    .dependsOn(coreProject)

  lazy val json4sJacksonProject = Project("json4s", file("scala-jwt-json4s"))
    .settings(CommonSettings: _*)
    .settings(
      name := "json4s",
      libraryDependencies ++= Seq(json4sJackson)
    )
    .aggregate(coreProject)
    .dependsOn(coreProject)

  lazy val playJsonProject = Project("play-json", file("scala-jwt-play-json"))
    .settings(CommonSettings: _*)
    .settings(
      name := "play-json",
      libraryDependencies ++= Seq(playJson)
    )
    .aggregate(coreProject)
    .dependsOn(coreProject)

  lazy val playProject = Project("play", file("scala-jwt-play"))
    .settings(CommonSettings: _*)
    .settings(
      name := "play",
      libraryDependencies ++= Seq(play)
    )
    .aggregate(playJsonProject)
    .dependsOn(playJsonProject)
}
