name := """play-angular-standalone"""

version := "0.2.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play" % "0.2.0"
)
