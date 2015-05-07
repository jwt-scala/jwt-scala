name := """play-angular-standalone"""

version := "0.0.3"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers += Resolver.bintrayRepo("pauldijou", "maven")

libraryDependencies ++= Seq(
  "pdi" %% "jwt-play" % "0.0.3"
)
