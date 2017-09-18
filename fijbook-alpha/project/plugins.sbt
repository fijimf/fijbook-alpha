// Comment to get more information during initialization
//logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.2")

addSbtPlugin("com.jamesward" %% "play-auto-refresh" % "0.0.14")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "1.2.1")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

