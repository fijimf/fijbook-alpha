package com.fijimf.deepfij.stats.spark

import java.util.{Date, UUID}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, DescribeClusterRequest, JobFlowInstancesConfig, ListClustersRequest, RunJobFlowRequest, StepConfig}
import com.amazonaws.services.elasticmapreduce.util.StepFactory

object ClusterManager {
  val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
    .withCredentials(new DefaultAWSCredentialsProviderChain())
    .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com", "us-east-1"))
    .build()

  val enableDebugging: StepConfig = new StepConfig()
    .withName("Enable debugging")
    .withActionOnFailure("TERMINATE_JOB_FLOW")
    .withHadoopJarStep(new StepFactory().newEnableDebuggingStep)

  val spark: Application = new Application().withName("Spark")

  def runSteps(clusterName:String, steps: Array[StepConfig]): Unit = {

    val request = new RunJobFlowRequest()
      .withName(clusterName)
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
    import scala.collection.JavaConversions._
    val now = new Date()
    val name = recreateAllStatistics()
    while (true) {
      emr.listClusters(new ListClustersRequest().withCreatedAfter(now)).getClusters.find(_.getName == name) match {
        case Some(csum) =>
          val describeClusterResult = emr.describeCluster(new DescribeClusterRequest().withClusterId(csum.getId))
          println(new Date()+"===============================================")
          println(csum.getId)
          println(csum.getName)
          println(csum.getNormalizedInstanceHours)
          println(csum.getStatus.getState)
          println(csum.getStatus.getTimeline.getCreationDateTime)
          println(csum.getStatus.getTimeline.getReadyDateTime)
          println(csum.getStatus.getTimeline.getEndDateTime)
      }
      Thread.sleep(20000L)
    }
  }
  
  
  def recreateAllStatistics(): String ={
    val name = "DF-"+UUID.randomUUID().toString
    val dbOptions = Map(
      StatsDbAccess.USER_KEY -> System.getenv("SPARK_DEEPFIJDB_USER"),
      StatsDbAccess.PASSWORD_KEY -> System.getenv("SPARK_DEEPFIJDB_PASSWORD")
    )
    runSteps(
      name,
      Array(
        enableDebugging,
        GenerateSnapshotParquetFiles.stepConfig(Map.empty[String, String]),
        WonLost.stepConfig(dbOptions),
        Scoring.stepConfig(dbOptions),
        MarginRegression.stepConfig(dbOptions)
      )
    )
    name
  }
}