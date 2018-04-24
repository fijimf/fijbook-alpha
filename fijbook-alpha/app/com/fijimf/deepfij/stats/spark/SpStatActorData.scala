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

case class SparkStatActorData(list: List[StatClusterSummary], listeners: List[ActorRef]) {
  def isRunning: Boolean = list.nonEmpty && StringUtils.isBlank(list.head.terminated)

  def activeCluster(): Option[String] = if (isRunning) Some(list.head.name) else None
}
