package com.fijimf.deepfij.stats.spark

import java.io.IOException

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain, PropertiesCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.elasticmapreduce._
import com.amazonaws.services.elasticmapreduce.model.{AddJobFlowStepsRequest, AddJobFlowStepsResult, HadoopJarStepConfig, JobFlowInstancesConfig, RunJobFlowRequest, StepConfig}
import com.amazonaws.services.elasticmapreduce.util.StepFactory
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.fijimf.deepfij.models.services.ScheduleSerializer


object ClusterManager {
  def main(args: Array[String]): Unit = {

   
    val emr: AmazonElasticMapReduce = AmazonElasticMapReduceClientBuilder.standard()
      .withCredentials(new DefaultAWSCredentialsProviderChain())
      .withEndpointConfiguration(new EndpointConfiguration("elasticmapreduce.amazonaws.com","us-east-1"))
      .build()
    val result = emr.listClusters()
    println(result.toString)

    val config = new JobFlowInstancesConfig().withInstanceCount(3)
    emr.runJobFlow(new RunJobFlowRequest("Step", config))
  }
}