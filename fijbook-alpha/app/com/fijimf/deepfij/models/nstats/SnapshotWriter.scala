package com.fijimf.deepfij.models.nstats

import akka.actor.{Actor, ActorPath, ActorRef, ActorSelection, Props}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.Logger

import scala.util.{Failure, Success}

object FeedMe
object DataAvailable
object SaveComplete

class SnapshotBuffer(target:ActorRef) extends Actor {
val log = Logger(this.getClass)

  override def receive = empty

  def empty:Receive = {
    case snap:SnapshotDbBundle=>
      log.info("Buffer in empty state.  Received snap bundle.  Becoming processing and notifying writer that data is available")
      context become processing(List(snap))
      target ! DataAvailable
    case FeedMe=>
      log.info("Buffer in empty state. Received a 'FeedMe'.  Ignoring b/c buffer is empty")
  }

  def processing(snaps:List[SnapshotDbBundle]):Receive ={
    case snap:SnapshotDbBundle=>
      log.info(s"Buffer in processing state.  Appending snapshot bundle.  New size is ${snaps.size+1}")
      context become processing(snap::snaps)
    case FeedMe =>
      log.info(s"Buffer in processing state. Got a FeedMe.  Sending batch of size  ${snaps.size}")
      sender ! snaps
      context become empty
  }
}

class SnapshotDAOWriter(dao:ScheduleDAO) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  override def receive = ready

  def ready: Receive = {
    case DataAvailable => sender ! FeedMe
    case snaps:List[SnapshotDbBundle] =>
      val capture = self
      context become busy(sender())
      dao.saveBatchedSnapshots(snaps).onComplete{
        case Success(o)=> capture ! SaveComplete
        case Failure(thr)=>
      }

  }

  def busy(tgt:ActorRef):Receive = {
    case SaveComplete =>
      context become ready
      tgt ! FeedMe
    case msg=>
      println(msg.toString)
  }
}
