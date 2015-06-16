name := """play-angular-standalone"""

version := "0.2.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.pauldijou" %% "jwt-play" % "0.3.0"
)
