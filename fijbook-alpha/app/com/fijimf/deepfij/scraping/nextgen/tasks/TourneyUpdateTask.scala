package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import akka.actor.ActorRef
import com.fijimf.deepfij.models.Game
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.nextgen.SSTask
import play.api.Logger

import scala.concurrent.Future
import scala.io.Source

final case class TourneyUpdateTask(filename: String, dao: ScheduleDAO) extends SSTask[Int] {
  val logger = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  def run(messageListener: Option[ActorRef]): Future[Int] = {
    TourneyUpdater.updateNcaaTournamentGames(filename, dao).map(_.size)
  }


  override def safeToRun: Future[Boolean] = Future.successful(true)

  override def name: String = "ID Tournament Games"

}


