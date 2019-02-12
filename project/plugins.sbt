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

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.0")

addSbtPlugin("com.typesafe.sbt"  % "sbt-site"               % "1.3.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-ghpages"            % "0.6.2")
addSbtPlugin("org.tpolecat"      % "tut-plugin"             % "0.6.0")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph"   % "0.9.0")
addSbtPlugin("com.jsuereth"      % "sbt-pgp"                % "1.1.1")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"           % "2.0")
