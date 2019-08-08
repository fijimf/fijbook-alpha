name := """fijbook-alpha"""

scalaVersion in ThisBuild := "2.12.8"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "com.fijimf.deepfij"
  )

buildInfoOptions += BuildInfoOption.BuildTime

scalacOptions += "-Ypartial-unification"

lazy val AWS_ACCESS_KEY_ID = sys.env.getOrElse("AWS_ACCESS_KEY_ID", "")
lazy val AWS_SECRET_KEY = sys.env.getOrElse("AWS_SECRET_KEY", "")
lazy val AWS_SMTP_USERNAME = sys.env.getOrElse("AWS_SMTP_USERNAME", "")
lazy val AWS_SMTP_PASSWORD = sys.env.getOrElse("AWS_SMTP_PASSWORD", "")

routesGenerator := InjectedRoutesGenerator

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/content/repositories/atlassian-public/"
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Apache" at "https://repository.apache.org/content/repositories/releases"
resolvers += Resolver.bintrayRepo("cibotech", "public")


dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"
dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.6.5"

lazy val doobieVersion = "0.7.0"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2"   % doobieVersion,
  "org.tpolecat" %% "doobie-scalatest"  % doobieVersion
)

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  ehcache,
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "com.typesafe.play" %% "play-iteratees" % "2.6.1",
  "com.h2database" % "h2" % "1.4.187",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2",
  "org.webjars" %% "webjars-play" % "2.7.0",
  "org.webjars" % "bootstrap" % "4.1.2",
  "org.webjars" % "jquery" % "3.3.1-1",
  "org.webjars" % "font-awesome" % "5.2.0",
  "org.webjars.npm" % "feather-icons" % "4.7.3",
  "org.webjars" % "popper.js" % "1.14.4",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test exclude  ("org.slf4j", "slf4j-simple"),
  specs2 % Test
)

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % "5.0.6",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.6",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.6",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.6",
  "net.codingwell" %% "scala-guice" % "4.2.3",
  "com.iheart" %% "ficus" % "1.4.6",
  "com.typesafe.play" %% "play-mailer" % "6.0.0",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.0",
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x",
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.6" % "test",
  "com.typesafe.akka" %% "akka-contrib" % "2.5.23",
  "com.typesafe.akka" %% "akka-agent" % "2.5.23",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.apache.commons" % "commons-text" % "1.1",
  "org.scalanlp" %% "breeze" % "0.13.2",
  "org.scalanlp" %% "breeze-natives" % "0.13.2",
  "org.apache.mahout" % "mahout-math" % "0.12.2",
  "org.apache.mahout" % "mahout-mr" % "0.12.2",
  "org.apache.hadoop" % "hadoop-client" % "2.7.3",
  "org.apache.hadoop" % "hadoop-aws" % "2.7.3",
  "org.aspectj" % "aspectjweaver" % "1.8.9",
  "com.amazonaws" % "aws-java-sdk" % "1.11.297",
  "com.vladsch.flexmark" % "flexmark-all" % "0.27.0",
  "org.typelevel" %% "cats-core" % "1.4.0" ,
  "org.typelevel" %% "cats-free" % "1.4.0",
  "com.github.haifengl" %% "smile-scala" % "1.5.1"
)
test in assembly := {}
mainClass in assembly := Some("play.core.server.ProdServerStart")
assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last endsWith ".conf" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.last
  case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.last
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith "messages" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith "types" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter {_.data.getName.startsWith("org.apache.spark")}
}
assemblyOutputPath in assembly :=  file(s"${sys.env.getOrElse("DEPLOY_DIR", "/tmp")}/${name.value}-${version.value}-assembly.jar")

import com.typesafe.sbt.packager.SettingsHelper._

enablePlugins(DockerPlugin)
dockerEntrypoint := Seq(
  "/opt/docker/bin/fijbook-alpha",
  "-J-Xmx2G",
  "-J-Xms1G",
  "-Dhttp.port=80",
  "-Dplay.http.secret.key='QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241AB`R5W:1uDFN];Ik@n'",
  s"-Dplay.mailer.user=$AWS_SMTP_USERNAME",
  s"-Dplay.mailer.password='$AWS_SMTP_PASSWORD'",
  "-Dplay.mailer.from=deepfij@gmail.com",
  "-Dadmin.user=fijimf@gmail.com",
  "-Daws.region=us-east-1",
  s"-Daws.accessKeyId=$AWS_ACCESS_KEY_ID ",
  s"-Daws.secretKey=$AWS_SECRET_KEY",
  "-Duser.timezone=America/New_York",
  "-Duser.home=/home/ec2-user",
  "-Dlogger.resource=prod-logback.xml")

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


wartremoverWarnings ++= Warts.allBut(Wart.NonUnitStatements)
import play.twirl.sbt.Import.TwirlKeys
wartremoverExcluded += (target in TwirlKeys.compileTemplates).value
wartremoverExcluded ++= routes.in(Compile).value

testOptions in Test += Tests.Argument("-oD")