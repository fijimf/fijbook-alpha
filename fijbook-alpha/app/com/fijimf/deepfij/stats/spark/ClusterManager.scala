package com.fijimf.deepfij.stats.spark

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, JobFlowInstancesConfig, RunJobFlowRequest, StepConfig}
import com.amazonaws.services.elasticmapreduce.util.StepFactory

object ClusterManager {
  val CLUSTER_NAME = "DeepFij Stats transient cluster"
  val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com", "us-east-1"))
    .build()

  val enableDebugging: StepConfig = new StepConfig()
    .withName("Enable debugging")
    .withActionOnFailure("TERMINATE_JOB_FLOW")
    .withHadoopJarStep(new StepFactory().newEnableDebuggingStep)

  val spark: Application = new Application().withName("Spark")

  def runSteps(steps: Array[StepConfig]): Unit = {

    val request = new RunJobFlowRequest()
      .withName(CLUSTER_NAME)
      .withReleaseLabel("emr-5.13.0")
      .withSteps(
        steps: _*
      ).withApplications(spark).withLogUri("s3://deepfij-emr/logs/")
      .withServiceRole("EMR_DefaultRole")
      .withJobFlowRole("EMR_EC2_DefaultRole")
      .withInstances(
        new JobFlowInstancesConfig()
          .withInstanceCount(3)
          .withKeepJobFlowAliveWhenNoSteps(false)
          .withMasterInstanceType("m3.xlarge")
          .withSlaveInstanceType("m3.xlarge")
      ).withLogUri("s3://deepfij-emr/logs/")
    emr.runJobFlow(request)
  }

  def main(args: Array[String]): Unit = {
    val dbOptions = Map(
      "stats.deepfijdb.user"-> System.getenv("SPARK_DEEPFIJDB_USER"),
      "stats.deepfijdb.password"-> System.getenv("SPARK_DEEPFIJDB_PASSWORD")
    )
    runSteps(Array(enableDebugging, GenerateSnapshotParquetFiles.stepConfig(Map.empty[String, String]), WonLost.stepConfig(dbOptions)))
  }
}