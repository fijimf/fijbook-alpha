package com.fijimf.deepfij.scraping.nextgen.tasks

import akka.actor.ActorRef
import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.nextgen.SSTask

import scala.concurrent.Future

final case class CreateSeasons(start: Int, end: Int, dao: ScheduleDAO) extends SSTask[Int]{
  import scala.concurrent.ExecutionContext.Implicits.global
  def run(messageListener: Option[ActorRef]): Future[Int] = {
    dao.saveSeasons(start.to(end).map(y => Season(0L, y)).toList).map(_.size)
  }

  val name: String = "Create seasons"

  def safeToRun: Future[Boolean] = Future.sequence(start.to(end).map(y =>dao.findSeasonByYear(y))).map(_.flatten.isEmpty)

}

