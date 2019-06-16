name := """play-angular-standalone"""

version := "3.0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.7"

routesGenerator := play.sbt.routes.RoutesKeys.InjectedRoutesGenerator

libraryDependencies ++= Seq(
  guice,
  "com.pauldijou"     %% "jwt-play" % "2.1.0"
)
