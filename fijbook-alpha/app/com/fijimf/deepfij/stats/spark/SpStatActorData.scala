package com.fijimf.deepfij.stats.spark

import akka.actor.ActorRef
import org.apache.commons.lang3.StringUtils

sealed trait SpStatActorCommand {
  val prefix: String
}

object GenerateParquetSnapshot extends SpStatActorCommand {
  override val prefix: String = "SNAP"
}

object GenerateTeamStatistics extends SpStatActorCommand {
  override val prefix: String = "DF"
}

object GenerateAll extends SpStatActorCommand {
  override val prefix: String = "ALL"
}

object SpStatActorCommand {
  def apply(prefix: String): SpStatActorCommand = {
    prefix match {
      case GenerateParquetSnapshot.prefix => GenerateParquetSnapshot
      case GenerateTeamStatistics.prefix => GenerateTeamStatistics
      case GenerateAll.prefix => GenerateAll
    }
  }
}

sealed trait SparkStatClusterStatus

case object NotRunning extends SparkStatClusterStatus {
  override def toString: String = "Ready"
}

case object OrphanRequest extends SparkStatClusterStatus{
  override def toString: String = "Unknown Request"
}

case object RunningRequest extends SparkStatClusterStatus{
  override def toString: String = "Running Request"
}

case class SparkStatActorData(name: Option[String], list: List[StatClusterSummary], listeners: List[ActorRef]) {

  def isAnyClusterRunning: Boolean = list.nonEmpty && StringUtils.isBlank(list.head.terminated)

  def status: SparkStatClusterStatus = {
    activeCluster match {
      case Some(clusterName) if name.contains(clusterName) => RunningRequest
      case Some(clusterName) if !name.contains(clusterName) => OrphanRequest
      case None => NotRunning
    }
  }

  def activeCluster: Option[String] = if (isAnyClusterRunning) Some(list.head.name) else None
}
