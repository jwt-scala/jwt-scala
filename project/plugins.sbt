resolvers ++= Seq(
  Resolver.url("bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases")
  )(Resolver.ivyStylePatterns),
  Resolver.url(
  "tpolecat-sbt-plugin-releases",
    url("http://dl.bintray.com/content/tpolecat/sbt-plugin-releases")
  )(Resolver.ivyStylePatterns),
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release"            % "1.0.4")
addSbtPlugin("com.typesafe.sbt"  % "sbt-site"               % "1.2.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"            % "0.5.4")
addSbtPlugin("org.tpolecat"      % "tut-plugin"             % "0.4.8")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph"   % "0.8.2")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                % "1.0.1")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"           % "1.1")
