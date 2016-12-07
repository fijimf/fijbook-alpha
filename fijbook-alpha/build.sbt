name := """fijbook-alpha"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.h2database" % "h2" % "1.4.187",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2",
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars" % "bootstrap" % "3.3.7-1",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "font-awesome" % "4.7.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  specs2 % Test
)

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "4.0.0",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "4.0.0",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "4.0.0",
  "com.mohiva" %% "play-silhouette-persistence" % "4.0.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.typesafe.play" %% "play-mailer" % "5.0.0",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % "test",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.15"
)



fork in Test := true
javaOptions in Test += "-Dconfig.resource=application-test.conf"