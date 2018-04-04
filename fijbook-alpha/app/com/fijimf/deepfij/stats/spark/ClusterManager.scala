package com.fijimf.deepfij.stats.spark

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{Application, HadoopJarStepConfig, JobFlowInstancesConfig, ListStepsRequest, RunJobFlowRequest, StepConfig}
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

object ClusterManager {

  def buildClasspath: String = {

    val s3: AmazonS3 = AmazonS3ClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration( "s3.amazonaws.com",  "us-east-1"))
      .build()
    val summaries = s3.listObjects("deepfij-spark-libs").getObjectSummaries
    0.until(summaries.size()).map(summaries.get).filterNot(_.getKey.startsWith("fijbook")).map(os => s"s3://deepfij-spark-libs/${os.getKey}").mkString(":")
  }
  
  def main(args: Array[String]): Unit = {

   
    val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com","us-east-1"))
      .build()

    
    val stepFactory = new StepFactory
    val enabledebugging = new StepConfig()
      .withName("Enable debugging")
      .withActionOnFailure("TERMINATE_JOB_FLOW")
      .withHadoopJarStep(stepFactory.newEnableDebuggingStep)

    val sparkClassPath = buildClasspath
    val runWonLost = new StepConfig()
      .withName("Run Won Lost")
      .withActionOnFailure("TERMINATE_JOB_FLOW")
      .withHadoopJarStep(
        new HadoopJarStepConfig()
          .withJar("command-runner.jar")
          .withArgs(
            "spark-submit",
            "--class", "com.fijimf.deepfij.stats.spark.WonLost",
            "--master", "yarn",
            "--deploy-mode", "cluster",
            "--executor-memory", "5g",
            "--num-executors", "10", 
            "--driver-class-path", sparkClassPath,
            "s3://deepfij-spark-libs/fijbook-alpha.fijbook-alpha-1.18-sans-externalized.jar",
            "1000"
          )
      )
    val spark = new Application().withName("Spark")

    val request = new RunJobFlowRequest()
      .withName("DeepFij Stats transient cluster")
      .withReleaseLabel("emr-5.3.1")
      .withSteps(enabledebugging, runWonLost).withApplications(spark).withLogUri("s3://deepfij-emr/logs/")
      .withServiceRole("EMR_DefaultRole")
      .withJobFlowRole("EMR_EC2_DefaultRole")
      .withInstances(
        new JobFlowInstancesConfig()
          .withInstanceCount(3)
          .withKeepJobFlowAliveWhenNoSteps(false)
          .withMasterInstanceType("m3.xlarge")
          .withSlaveInstanceType("m3.xlarge")
      ).withLogUri("s3://deepfij-emr/logs/")

    
    val result = emr.runJobFlow(request)
    System.out.println("This is result: " + result.toString)
    val resultX = emr.listClusters()
    println(resultX.toString)
    val resultY = emr.listSteps(new ListStepsRequest().withClusterId(result.getJobFlowId))
    println(resultY.toString)
  }
}