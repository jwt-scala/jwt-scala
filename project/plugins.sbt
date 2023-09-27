// Documentation

addSbtPlugin("com.github.sbt" % "sbt-site-paradox" % "1.5.0")

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")

addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")

addSbtPlugin(
  ("com.github.sbt" % "sbt-ghpages" % "0.8.0")
    // sbt-ghpages depends on sbt-site 1.4.1, which pulls Scala XML 1.x
    .exclude("org.scala-lang.modules", "scala-xml_2.12")
)

// Publishing

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

// Linting

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("org.typelevel" % "sbt-tpolecat" % "0.5.0")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.3")

// Scala JS / Scala Native

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.14.0")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.15")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.2")
