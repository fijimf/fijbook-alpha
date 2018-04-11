package com.fijimf.deepfij.stats.spark

import com.amazonaws.services.elasticmapreduce.model.{HadoopJarStepConfig, StepConfig}

trait SparkStepConfig {
  val ASSEMBLY_JAR = "s3://deepfij-spark-libs/fijbook-alpha-assembly.jar"

  def createStepConfig(name: String, fqn: String, extraOptions: Map[String, String]) = {
    new StepConfig()
      .withName(name)
      .withActionOnFailure("TERMINATE_JOB_FLOW")
      .withHadoopJarStep(
        new HadoopJarStepConfig()
          .withJar("command-runner.jar")
          .withArgs(
            createArgs(name, fqn, extraOptions): _*
          )
      )
  }

  def createArgs(name: String, fqn: String, extraOptions: Map[String, String]): Array[String] = {
    val dOptions = extraOptions.map { case (k: String, v: String) => s"-D$k=$v" }.mkString(" ")
    if (extraOptions.isEmpty)
      Array(
        "spark-submit",
        "--class", fqn,
        "--master", "yarn",
        "--deploy-mode", "cluster",
        "--executor-memory", "5g",
        "--num-executors", "10",
        ASSEMBLY_JAR
      )
    else
      Array(
        "spark-submit",
        "--class", fqn,
        "--conf", s"spark.driver.extraJavaOptions=$dOptions",
        "--conf", s"spark.executor.extraJavaOptions=$dOptions",
        "--master", "yarn",
        "--deploy-mode", "cluster",
        "--executor-memory", "5g",
        "--num-executors", "10",
        ASSEMBLY_JAR
      )

  }
  
  def stepConfig(extraProperties:Map[String, String]): StepConfig
}
