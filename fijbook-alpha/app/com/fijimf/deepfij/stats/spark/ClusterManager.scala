package com.fijimf.deepfij.stats.spark

import java.util.{Date, UUID}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, ClusterState, JobFlowInstancesConfig, ListClustersRequest, RunJobFlowRequest, StepConfig}
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

  val dbOptions = Map(
    StatsDbAccess.USER_KEY -> System.getenv("SPARK_DEEPFIJDB_USER"),
    StatsDbAccess.PASSWORD_KEY -> System.getenv("SPARK_DEEPFIJDB_PASSWORD")
  )

  def runSteps(clusterName: String, steps: Array[StepConfig]): Unit = {

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


  def generateSnapshotParquetFiles(): (String, Date) = {
    val name = "SNAP-" + UUID.randomUUID().toString

    runSteps(
      name,
      Array(
        enableDebugging,
        GenerateSnapshotParquetFiles.stepConfig(Map.empty[String, String])
      )
    )
    (name, new Date())
  }

  def generateTeamStatistics(): (String, Date) = {
    val name = "DF-" + UUID.randomUUID().toString

    runSteps(
      name,
      Array(
        enableDebugging,
        WonLost.stepConfig(dbOptions),
        Scoring.stepConfig(dbOptions),
        MarginRegression.stepConfig(dbOptions)
      )
    )
    (name, new Date())
  }

  def recreateAll(): (String, Date) = {
    val name = "ALL-" + UUID.randomUUID().toString

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
    (name, new Date())
  }

  def isClusterRunning(name: String, start: Date): Boolean = {
    getClusterState(name, start) match {
      case Some(c) => c == ClusterState.TERMINATED || c == ClusterState.TERMINATED_WITH_ERRORS
      case None => false
    }
  }

  def getClusterState(name: String, start: Date): Option[ClusterState] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest().withCreatedAfter(start)).getClusters.find(_.getName == name).map(_.getStatus.getState).map(ClusterState.valueOf(_))
  }
}