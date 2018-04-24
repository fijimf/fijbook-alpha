package com.fijimf.deepfij.stats.spark

import akka.actor.{ActorRef, FSM}
import org.apache.commons.lang3.StringUtils

import scala.concurrent.duration._


case object SpStatStatus

case object SpStatGenFiles

case object SpStatGenStats

case object SpStatGenAll

case object SpStatCancel




class SpStatActor() extends FSM[SparkStatClusterStatus, SparkStatActorData] {

  private val initState = SparkStatActorData( None, ClusterManager.getClusterList, List.empty[ActorRef])
 
  startWith(initState.status,initState )

  when(NotRunning) {
    case Event(SpStatStatus, r: SparkStatActorData) =>
      if (!r.listeners.contains(sender())) {
        evaluateClusterStatus(r.copy(listeners=sender() :: r.listeners))
      } else {
        evaluateClusterStatus(r)
      }

    case Event(SpStatGenFiles, _) => 
      val (name, _) = ClusterManager.generateSnapshotParquetFiles()
      val data = SparkStatActorData(Some(name), ClusterManager.getClusterList, List(sender()))
      goto (data.status) using data
      
    case Event(SpStatGenStats, _) => 
      val (name, _) = ClusterManager.generateTeamStatistics()
      val data = SparkStatActorData(Some(name), ClusterManager.getClusterList, List(sender()))
      goto (data.status) using data
    
    case Event(SpStatGenAll, _) => 
      val (name, _) = ClusterManager.generateTeamStatistics()
      val data = SparkStatActorData(Some(name), ClusterManager.getClusterList, List(sender()))
      goto (data.status) using data
  }


  when(RunningRequest) {
    case Event(SpStatStatus, r: SparkStatActorData) =>
      if (!r.listeners.contains(sender())) {
        evaluateClusterStatus(r.copy(listeners=sender() :: r.listeners))
      } else {
        evaluateClusterStatus(r)
      }
      
    case Event(SpStatCancel, p: SparkStatActorData) =>
      p.activeCluster match {
        case Some(c)=>ClusterManager.getClusterSummary(c).map(_.getId).foreach(id => ClusterManager.terminateCluster(id))
      }
      evaluateClusterStatus(p)
    case Event(_, _) =>
      log.warning("Received a command while still running.")
      stay
  }
  
  when(OrphanRequest) {
    case Event(SpStatStatus, r: SparkStatActorData) =>
      if (!r.listeners.contains(sender())) {
        evaluateClusterStatus(r.copy(listeners=sender() :: r.listeners))
      } else {
        evaluateClusterStatus(r)
      }
      
    case Event(SpStatCancel, p: SparkStatActorData) =>
      p.activeCluster match {
        case Some(c)=>ClusterManager.getClusterSummary(c).map(_.getId).foreach(id => ClusterManager.terminateCluster(id))
      }
      evaluateClusterStatus(p)
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

  private def evaluateClusterStatus(d:SparkStatActorData): FSM.State[SparkStatClusterStatus, SparkStatActorData] = {
    val c = SparkStatActorData(d.name, ClusterManager.getClusterList, d.listeners)
    if (c.status == NotRunning)
      goto (c.status) using c.copy(name=None)
    else 
      goto (c.status) using c
  }


}
