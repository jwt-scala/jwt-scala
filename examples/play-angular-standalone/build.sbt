name := """play-angular-standalone"""

version := "2.1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.7"

routesGenerator := play.sbt.routes.RoutesKeys.InjectedRoutesGenerator

libraryDependencies ++= Seq(
  guice,
  "com.pauldijou"     %% "jwt-play" % "2.1.0"
)
