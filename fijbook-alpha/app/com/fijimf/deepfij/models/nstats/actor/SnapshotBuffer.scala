package com.fijimf.deepfij.models.nstats.actor

import akka.actor.{Actor, ActorRef, Props}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.actor.SnapshotBuffer.{NoWorkToDo, WriterReady}
import com.fijimf.deepfij.models.nstats.{SendingComplete, SnapshotDbBundle}
import play.api.Logger

import scala.concurrent.duration.Duration

object SnapshotBuffer {
  object WriterReady
  object NoWorkToDo
}

/**
  * The Snapshot Buffer manages writing to the database
  * -- it creates a child actor 'writer' whioch handles the actual writing to the DB
  * -- The initial state is
  *     a) An empty list of DB Bundles;
  *     b) A writer-ready flag set to true;
  *     c) An Option[ActorRef] which is empty
  * -- When we receive more data,
  *      If writer-ready is true, we add the data to the buffer, take a bunch of records from the buffer send them to the 'writer', and set writer-ready to false
  *      If writer-ready is false, we add the data to the buffer, and maintain writer-ready as false
  * -- When we receive FeedMe,
  *
  * @param dao
  */
class SnapshotBuffer(dao: ScheduleDAO) extends Actor {
  val log = Logger(this.getClass)

  val batchSize = 500
  val startTime: Long = System.nanoTime()
  val writer: ActorRef = context.actorOf(Props(classOf[SnapshotDAOWriter], dao))

  override def receive: Receive = buffer(List.empty[SnapshotDbBundle], writerReady = true, notifyTarget = None)

  def buffer(snaps: List[SnapshotDbBundle], writerReady: Boolean, notifyTarget: Option[ActorRef]): Receive = {
    case snap: SnapshotDbBundle =>
      if (snaps.size % 100 == 99) log.info(s"Buffer with data received snap bundle.  New size is ${snaps.size + 1}")
      if (writerReady) {
        val (front, back) = snaps.splitAt(batchSize)
        writer ! front
        context become buffer(back, writerReady = false, notifyTarget)
      } else {
        context become buffer(snap :: snaps, writerReady = false, notifyTarget)
      }
    case WriterReady =>
      val (front, back) = snaps.splitAt(batchSize)
      if (front.isEmpty) {
        notifyTarget match {
          case Some(tgt) =>
            log.info("Writer completed batch.  Buffer is empty.  Completion notify   Ready to die.  I'm literally dying")
            val d = Duration.fromNanos(System.nanoTime() - startTime)
            tgt ! s"Completed in ${d.toString()} "
            context stop self
          case None =>
            context become buffer(List.empty[SnapshotDbBundle], writerReady = true, notifyTarget)
        }
      } else {
        log.info(s"Writer asking for records.  Sending batch of size  ${front.size}")
        writer ! front
        log.info(s"Buffer is size ${back.size}.")
        context become buffer(back, writerReady = false, notifyTarget)
      }
    case SendingComplete =>
      log.info("Received a SendingComplete message.  Setting notifyTarget.")
      context become buffer(snaps, writerReady, notifyTarget = Some(sender()))
    case NoWorkToDo =>

    case dk: Any => log.warn(s"[hasData] DON'T KNOW: $dk")
  }
}


