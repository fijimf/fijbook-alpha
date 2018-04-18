package com.fijimf.deepfij.stats.spark

import akka.actor.{Actor, ActorRef, Props}
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.Logger
import play.api.libs.ws.WSClient

object SparkStatsManagerActor {
  def props(out: ActorRef,
            sparkStatActor: ActorRef,
            ws: WSClient,
            repo: ScheduleRepository,
            dao: ScheduleDAO
           ) = Props(new SparkStatsManagerActor(out, sparkStatActor, ws, repo, dao))
}

case class SparkStatsManagerActor(
                                   out: ActorRef,
                                   sparkStatActor: ActorRef,
                                   ws: WSClient,
                                   repo: ScheduleRepository,
                                   dao: ScheduleDAO
                                 ) extends Actor {


  val log = Logger(this.getClass)

  override def receive: Receive = {
    case (s: String) if s.toLowerCase == "$command::status" =>
      log.info("Received status command")
      sparkStatActor ! SpStatStatus
    case (s: String) if s.toLowerCase == "$command::generate_parquet" =>
      log.info("Received cancel task list command")
      sparkStatActor ! SpStatGenFiles
    case (s: String) if s.toLowerCase == "$command::generate_stats" =>
      log.info("Received cancel task command")
      sparkStatActor ! SpStatGenStats
    case (s: String) if s.toLowerCase == "$command::generate_all" =>
      log.info("Received status command")
      sparkStatActor ! SpStatGenAll
    case (s: String) if s.toLowerCase == "$command::cancel_spark_jobs" =>
      log.info("Received full rebuild command")
      sparkStatActor ! SpStatCancel
    case _ =>
      log.error("Received an unexpected message")
  }
}
