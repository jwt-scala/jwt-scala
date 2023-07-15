// Documentation

addSbtPlugin("com.github.sbt" % "sbt-site-paradox" % "1.5.0")

addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.6.0")

// Publishing

addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

// Linting

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.3")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.2")

// Scala JS / Scala Native

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.2")

addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.14")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.1")

addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "1.3.1")
