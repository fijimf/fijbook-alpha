// Comment to get more information during initialization
//logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.14")

addSbtPlugin("com.jamesward" %% "play-auto-refresh" % "0.0.14")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "1.2.1")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

