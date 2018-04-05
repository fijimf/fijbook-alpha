name := """fijbook-alpha"""

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.fijimf.deepfij"
  )

buildInfoOptions += BuildInfoOption.BuildTime

scalaVersion := "2.11.8"

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Kamon Repository Snapshots" at "http://snapshots.kamon.io"
resolvers += "Apache" at "https://repository.apache.org/content/repositories/releases"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"
dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.5"

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
  "com.typesafe.akka" %% "akka-agent" % "2.3.15",
  "com.chuusai" %% "shapeless" % "2.3.1",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.apache.commons" % "commons-text" % "1.1",
  "org.scalanlp" %% "breeze" % "0.12",
  "org.scalanlp" %% "breeze-natives" % "0.12",
  "org.apache.mahout" % "mahout-math" % "0.12.2",
  "org.apache.mahout" % "mahout-mr" % "0.12.2",
  "org.apache.hadoop" % "hadoop-client" % "2.7.3",
  "org.aspectj" % "aspectjweaver" % "1.8.9",
  "com.amazonaws" % "aws-java-sdk" % "1.11.106",
  "com.vladsch.flexmark" % "flexmark-all" % "0.27.0",
  "org.apache.spark" %% "spark-mllib" % "2.2.1"
)

test in assembly := {}
mainClass in assembly := Some("play.core.server.ProdServerStart")
assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last endsWith ".conf" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith "MANIFEST.MF" => MergeStrategy.discard
  case _ => MergeStrategy.last
}
assemblyOutputPath in assembly :=  file(s"${sys.env.getOrElse("DEPLOY_DIR", "/tmp")}/${name.value}-${version.value}-assembly.jar")

import com.typesafe.sbt.packager.SettingsHelper._

makeDeploymentSettings(Universal, packageBin in Universal, "zip")
publishTo := Some(Resolver.file("file", new File(sys.env.getOrElse("DEPLOY_DIR", "/tmp"))))
releaseIgnoreUntrackedFiles := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// ...

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runClean, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  // publishArtifacts,                    // : ReleaseStep, checks whether `publishTo` is properly set up
  ReleaseStep(releaseStepTask(publish in Universal)),
  ReleaseStep(releaseStepTask(assembly in Universal)),
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)

fork in Test := false
parallelExecution in Test := false
javaOptions in Test += "-Dconfig.resource=application-test.conf"

