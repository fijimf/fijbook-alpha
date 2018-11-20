package com.fijimf.deepfij.models.nstats.actor

import akka.actor.{Actor, ActorRef}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.SnapshotDbBundle
import com.fijimf.deepfij.models.nstats.actor.SnapshotBuffer.WriterReady
import play.api.Logger

import scala.util.{Failure, Success}

class SnapshotDAOWriter(dao: ScheduleDAO) extends Actor {

  object DataAvailable

  object SaveComplete

  val log = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = ready

  def ready: Receive = {
    case DataAvailable =>
      log.info("[ready] Received DataAvailable. Replying with FeedMe")
      sender ! WriterReady
    case snaps: List[SnapshotDbBundle] =>
      context become busy(sender())
      dao.insertSnapshots(snaps).onComplete {
        case Success(o) =>
          log.info(s"saveBatchedSnapshots succeeded with return value of $o")
          self ! SaveComplete
        case Failure(thr) =>
          log.error("saveBatchedSnapshots failed", thr)
      }
    case dk: Any => log.warn(s"[ready] DON'T KNOW: $dk")
  }

  def busy(tgt: ActorRef): Receive = {
    case SaveComplete =>
      context become ready
      tgt ! WriterReady
    case dk: Any => log.warn(s"[busy] DON'T KNOW: $dk")
  }
}
