package com.fijimf.deepfij.stats.spark

import akka.actor.{ActorRef, FSM}

import scala.concurrent.duration._


case object SpStatStatus

case object SpStatGenFiles

case object SpStatGenStats

case object SpStatGenAll

case object SpStatCancel

sealed trait SpStatActorState

case object ClusterReady extends SpStatActorState

case object ClusterActive extends SpStatActorState


class SpStatActor() extends FSM[SpStatActorState, SpStatActorData] {

  startWith(ClusterReady, ClusterNotRunning(ClusterManager.getClusterList, List.empty[ActorRef]))

  when(ClusterReady) {
    case Event(SpStatStatus, r: ClusterNotRunning) => if (!r.listeners.contains(sender())) {
      val r1 = r.copy(listeners = sender() :: r.listeners)
      stay replying r1
    } else {
      stay replying r
    }

    case Event(SpStatGenFiles, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateSnapshotParquetFiles()
      ClusterRunning(name, time, GenerateParquetSnapshot, List(sender()))
    }
    case Event(SpStatGenStats, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateSnapshotParquetFiles()
      ClusterRunning(name, time, GenerateParquetSnapshot, List(sender()))
    }
    case Event(SpStatGenAll, _) => goto(ClusterActive) using {
      val (name, time) = ClusterManager.generateSnapshotParquetFiles()
      ClusterRunning(name, time, GenerateParquetSnapshot, List(sender()))
    }
  }


  when(ClusterActive) {
    case Event(SpStatStatus, p: ClusterRunning) =>
      val q = if (!p.listeners.contains(sender())) {
        p.copy(listeners = sender() :: p.listeners)
      } else {
        p
      }
      evaluateClusterStatus(q)

    case Event(SpStatCancel, p: ClusterRunning) =>
      ClusterManager.getClusterSummary(p.clusterName, p.clusterTime).map(_.getId).foreach(id => ClusterManager.terminateCluster(id))
      evaluateClusterStatus(p)

    case Event(_, p: ClusterRunning) =>
      log.warning("Received a command while still running.")
      stay
  }

  onTransition {
    case ClusterReady -> ClusterActive =>
      println("Transform: Ready->Processing")
      nextStateData match {
        case (p: ClusterRunning) => p.listeners.foreach(_ ! p)
        case _ =>
      }
    case ClusterActive -> ClusterActive =>
      println("Transform: Processing->Processing")
      nextStateData match {
        case (p: ClusterRunning) => p.listeners.foreach(_ ! p)
        case _ =>
      }
    case ClusterActive -> ClusterReady =>
      println("Transform: Processing->Ready")
      (stateData, nextStateData) match {
        case (p: ClusterRunning, r: ClusterNotRunning) => p.listeners.foreach(_ ! r)
        case _ =>
      }
  }

  setTimer("StatusUpdater", SpStatStatus, 15.seconds, repeat = true)

  private def evaluateClusterStatus(q: ClusterRunning): FSM.State[SpStatActorState, SpStatActorData] = {
    if (ClusterManager.isClusterRunning(q.clusterName, q.clusterTime)) {
      stay using q replying q
    } else {
      goto(ClusterReady) using {
        ClusterNotRunning(ClusterManager.getClusterList, q.listeners)
      }
    }
  }


}
