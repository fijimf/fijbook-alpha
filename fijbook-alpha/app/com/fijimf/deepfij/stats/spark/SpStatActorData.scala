package com.fijimf.deepfij.stats.spark

import java.util.Date

import akka.actor.ActorRef
import com.amazonaws.services.elasticmapreduce.model.ClusterStatus

sealed trait SpStatActorCommand 

object GenerateParquetSnapshot extends SpStatActorCommand
object GenerateTeamStatistics extends SpStatActorCommand
object GenerateAll extends SpStatActorCommand

sealed trait SpStatActorData

case class ClusterNotRunning
(
  last: Option[ClusterStatus],
  listeners:List[ActorRef]
) extends SpStatActorData

case class ClusterRunning
(
  clusterName: String,
  clusterTime: Date,
  command:SpStatActorCommand,
  listeners:List[ActorRef]
) extends SpStatActorData
