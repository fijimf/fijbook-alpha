name := """fijbook-alpha"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Kamon Repository Snapshots" at "http://snapshots.kamon.io"
resolvers += "Apache" at "https://repository.apache.org/content/repositories/releases"

libraryDependencies ++= Seq(
  ehcache,
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "com.typesafe.play" %% "play-iteratees" % "2.6.1",
  "com.h2database" % "h2" % "1.4.187",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2",
  "org.webjars" %% "webjars-play" % "2.6.0-M1",
  "org.webjars" % "bootstrap" % "3.3.7-1",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "font-awesome" % "4.7.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  specs2 % Test
)

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "5.0.0-RC3",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.0-RC3",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.0-RC3",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.0-RC3",
  "net.codingwell" %% "scala-guice" % "4.0.0",
  "net.ceedubs" %% "ficus" % "1.1.2",
   "com.typesafe.play" %% "play-mailer" % "6.0.0",
    "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x",
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3",
  "com.mohiva" %% "play-silhouette-testkit" % "4.0.0" % "test",
  "com.typesafe.akka" %% "akka-contrib" % "2.3.15",
  "com.chuusai" %% "shapeless" % "2.3.1",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.scalanlp" %% "breeze" % "0.12",
  "org.scalanlp" %% "breeze-natives" % "0.12",
  "org.apache.mahout" % "mahout-math" % "0.12.2",
  "org.apache.mahout" % "mahout-mr" % "0.12.2",
  "org.apache.hadoop" % "hadoop-client" % "2.7.3",

//  "io.kamon" %% "kamon-core" % "0.6.3",
//  "io.kamon" %% "kamon-scala" % "0.6.3",
//  "io.kamon" %% "kamon-play-25" % "0.6.3",
//  "io.kamon" %% "kamon-log-reporter" % "0.6.3",
  "org.aspectj" % "aspectjweaver" % "1.8.9",
"com.amazonaws"%"aws-java-sdk"%"1.11.106"
)



fork in Test := false
parallelExecution in Test := false
javaOptions in Test += "-Dconfig.resource=application-test.conf"