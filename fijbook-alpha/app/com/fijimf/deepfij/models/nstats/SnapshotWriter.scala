package com.fijimf.deepfij.models.nstats

import akka.actor.{Actor, ActorRef, Props}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.Logger

import scala.util.{Failure, Success}

object FeedMe
object DataAvailable
object SaveComplete

object CalculationComplete

class SnapshotBuffer(dao: ScheduleDAO) extends Actor {
val log = Logger(this.getClass)
  val batchSize = 100
  val startTime = System.currentTimeMillis()

  val writer = context.actorOf(Props(classOf[SnapshotDAOWriter], dao))

  override def receive = buffer(List.empty[SnapshotDbBundle], writerReady = true, readyToDie = false)

  def buffer(snaps: List[SnapshotDbBundle], writerReady: Boolean, readyToDie: Boolean): Receive = {
    case snap: SnapshotDbBundle =>
      if (snaps.size % 100 == 99) log.info(s"Buffer with data received snap bundle.  New size is ${snaps.size + 1}")
      if (writerReady) {
        val (front, back) = snaps.splitAt(batchSize)
        writer ! front
        context become buffer(back, writerReady = false, readyToDie)
      } else {
        context become buffer(snap :: snaps, writerReady = false, readyToDie)
      }
    case FeedMe =>
      val (front, back) = snaps.splitAt(batchSize)
      if (front.isEmpty) {
        if (readyToDie) {
          log.info("No snaps.  Done Writing.  Ready to die.  I'm literally dying")
          sender() ! s"Completed in ${startTime - System.currentTimeMillis()} ms"
          context stop self
        } else {
          context become buffer(List.empty[SnapshotDbBundle], writerReady = true, readyToDie)
        }
      } else {
        log.info(s"Writer asking for records.  Sending batch of size  ${front.size}")
        writer ! front
        log.info(s"Buffer is size ${back.size}.")
        context become buffer(back, writerReady = false, readyToDie)
      }
    case CalculationComplete =>
      log.info("Received a CalculationComplete.  I AM READY TO DIE")
      context become buffer(snaps, writerReady, readyToDie = true)
    case dk: Any => log.warn(s"[hasData] DON'T KNOW: $dk")
  }
}

class SnapshotDAOWriter(dao:ScheduleDAO) extends Actor {
  val log = Logger(this.getClass)
  import scala.concurrent.ExecutionContext.Implicits.global
  override def receive = ready

  def ready: Receive = {
    case DataAvailable =>
      log.info("[ready] Received DataAvailable. Replying with FeedMe")
      sender ! FeedMe
    case snaps:List[SnapshotDbBundle] =>
      val capture = self
      context become busy(sender())
      dao.saveBatchedSnapshots(snaps).onComplete{
        case Success(o) =>
          log.info(s"saveBatchedSnapshots succeeded with return value of $o")
          capture ! SaveComplete
        case Failure(thr)=>
          log.error("saveBatchedSnapshots failed", thr)
      }
    case dk: Any => log.warn(s"[ready] DON'T KNOW: $dk")
  }

  def busy(tgt:ActorRef):Receive = {
    case SaveComplete =>
      context become ready
      tgt ! FeedMe
    case dk: Any => log.warn(s"[busy] DON'T KNOW: $dk")
  }
}
