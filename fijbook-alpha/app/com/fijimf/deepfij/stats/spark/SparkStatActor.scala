package com.fijimf.deepfij.stats.spark

import akka.actor.{ActorRef, FSM}
import org.apache.commons.lang3.StringUtils

import scala.concurrent.duration._


case object SpStatStatus

case object SpStatGenFiles

case object SpStatGenStats

case object SpStatGenAll

case object SpStatCancel

sealed trait SparkStatActorState

case object ClusterReady extends SparkStatActorState

case object ClusterActive extends SparkStatActorState


class SpStatActor() extends FSM[SparkStatActorState, SparkStatActorData] {

  private val initState = SparkStatActorData( ClusterManager.getClusterList, List.empty[ActorRef])
 
  startWith(if (initState.isRunning) ClusterActive else ClusterReady,initState )

  when(ClusterReady) {
    case Event(SpStatStatus, r: SparkStatActorData) => if (!r.listeners.contains(sender())) {
      evaluateClusterStatus(sender() :: r.listeners)
    } else {
      evaluateClusterStatus(r.listeners)
    }

    case Event(SpStatGenFiles, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateSnapshotParquetFiles()
      SparkStatActorData(ClusterManager.getClusterList, List(sender()))
    }
    case Event(SpStatGenStats, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateTeamStatistics()
      SparkStatActorData(ClusterManager.getClusterList, List(sender()))
    }
    case Event(SpStatGenAll, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateTeamStatistics()
      SparkStatActorData(ClusterManager.getClusterList, List(sender()))
    }
  }


  when(ClusterActive) {
    case Event(SpStatStatus, r: SparkStatActorData) => if (!r.listeners.contains(sender())) {
      evaluateClusterStatus(sender() :: r.listeners)
    } else {
      evaluateClusterStatus(r.listeners)
    }

    case Event(SpStatCancel, p: SparkStatActorData) =>
      p.activeCluster() match {
        case Some(c)=>ClusterManager.getClusterSummary(c).map(_.getId).foreach(id => ClusterManager.terminateCluster(id))
      }
      evaluateClusterStatus(p.listeners)
    case Event(_, _) =>
      log.warning("Received a command while still running.")
      stay
  }

  onTransition {
    case _ =>
      nextStateData match {
        case (p: SparkStatActorData) => p.listeners.foreach(_ ! p)
        case _ =>
      }
  }

  setTimer("StatusUpdater", SpStatStatus, 15.seconds, repeat = true)

  private def evaluateClusterStatus(p:List[ActorRef]): FSM.State[SparkStatActorState, SparkStatActorData] = {
    val c = SparkStatActorData(ClusterManager.getClusterList, p)
    if (c.isRunning) {
      goto(ClusterActive) using c
    } else {
      goto(ClusterReady) using c
    }
  }


}
