package com.fijimf.deepfij.scraping.nextgen.tasks

import akka.actor.ActorRef
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.dao.schedule.util.ScheduleUtil
import com.fijimf.deepfij.scraping.nextgen.SSTask

import scala.concurrent.Future

case class SeedConferenceMaps(dao: ScheduleDAO) extends SSTask[Int] {
  import scala.concurrent.ExecutionContext.Implicits.global
  def run(messageListener: Option[ActorRef]): Future[Int] = {
    for {
      _ <- dao.deleteAllConferenceMaps()
      lcm <- ScheduleUtil.createConferenceMapSeeds(dao, id)
      _ <- dao.saveConferenceMaps(lcm)
    } yield {
      lcm.size
    }
  }

  val name: String = "Seed conference maps"

  val safeToRun: Future[Boolean] = Future.successful(true)
}




