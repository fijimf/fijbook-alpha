name := """fijbookLibrary"""

scalaVersion := "2.11.8"

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Kamon Repository Snapshots" at "http://snapshots.kamon.io"
resolvers += "Apache" at "https://repository.apache.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  specs2 % Test
)

