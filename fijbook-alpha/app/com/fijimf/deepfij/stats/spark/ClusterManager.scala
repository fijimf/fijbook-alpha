package com.fijimf.deepfij.stats.spark

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.{Date, UUID}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, Cluster, ClusterState, ClusterStatus, ClusterSummary, DescribeClusterRequest, JobFlowInstancesConfig, ListClustersRequest, ListStepsRequest, RunJobFlowRequest, StepConfig, TerminateJobFlowsRequest}
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

  def listActiveClusters(): List[Cluster] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest()).getClusters.map(cs => emr.describeCluster(new DescribeClusterRequest().withClusterId(cs.getId)).getCluster).toList
  }

  def terminateCluster(id: String): String = {
    import scala.collection.JavaConversions._
    val steps = emr.listSteps(new ListStepsRequest().withClusterId(id))
    val keys = steps.getSteps.filter(step => List("PENDING", "RUNNING").contains(step.getStatus.getState)).map(_.getId)
    val result = emr.terminateJobFlows(new TerminateJobFlowsRequest(keys))
    result.getSdkResponseMetadata.getRequestId
  }

  def isClusterRunning(name: String, start: Date): Boolean = {
    getClusterStatus(name, start).map(s => ClusterState.valueOf(s.getState)) match {
      case Some(c) => c == ClusterState.TERMINATED || c == ClusterState.TERMINATED_WITH_ERRORS
      case None => false
    }
  }

  def getClusterStatus(name: String, start: Date): Option[ClusterStatus] = {
    getClusterSummary(name, start).map(_.getStatus)
  }

  def getClusterSummary(name: String, start: Date): Option[ClusterSummary] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest().withCreatedAfter(start)).getClusters.find(_.getName == name)
  }

  def getClusterList:List[StatClusterSummary] = {
    import scala.collection.JavaConversions._
    emr.listClusters(new ListClustersRequest())
      .getClusters.filter(c => c.getName.startsWith("SNAP-") || c.getName.startsWith("DF-") || c.getName.startsWith("ALL-"))
      .sortBy(_.getStatus.getTimeline.getCreationDateTime.getTime * -1)
      .map(StatClusterSummary(_)).toList
  }
}

case class StatClusterSummary(name: String, status: String, creation: String, ready: String, terminated: String)
object StatClusterSummary {
  def apply(cs:ClusterSummary): StatClusterSummary = {
    val create = Option(cs.getStatus.getTimeline.getCreationDateTime).map(d=>new SimpleDateFormat("MMM-d HH:mm:ss").format(d)).getOrElse("")
    val ready = Option(cs.getStatus.getTimeline.getReadyDateTime).map(d=>new SimpleDateFormat("MMM-d HH:mm:ss").format(d)).getOrElse("")
    val end = Option(cs.getStatus.getTimeline.getEndDateTime).map(d=>new SimpleDateFormat("MMM-d HH:mm:ss").format(d)).getOrElse("")
    StatClusterSummary(cs.getName, cs.getStatus.getState,create, ready, end)
  }
}
