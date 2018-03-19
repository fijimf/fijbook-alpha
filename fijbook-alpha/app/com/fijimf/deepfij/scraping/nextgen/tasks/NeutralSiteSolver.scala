package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.LocalDateTime

import akka.actor.ActorRef
import com.fijimf.deepfij.models.{Game, Schedule}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.scraping.nextgen.SSTask

import scala.concurrent.Future

class NeutralSiteSolver(dao:ScheduleDAO)  extends SSTask[List[Game]]{

  def run(messageListener:Option[ActorRef]): Future[List[Game]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dao.loadSchedules()
      .map(schedules =>
        schedules.flatMap(neutralUpdatesForSchedule)
      )
      .flatMap(gl =>
        dao.updateGames(gl)
      )
  }

  private def neutralUpdatesForSchedule(sch: Schedule): List[Game] = {
    val locationData: Map[Long, Map[String, Int]] = sch.teamHomeGamesByLocation
    sch.games.foldLeft(List.empty[Option[Game]])((games: List[Option[Game]], game: Game) => {
      (for {
        location: String <- game.location
        homeGameSites: Map[String, Int] <- locationData.get(game.homeTeamId)
        timesAtLocation: Int <- homeGameSites.get(location)
        u: Game <- createNeutralUpdate(game, timesAtLocation)
      } yield {
        u
      }) :: games

    }).flatten
  }

  private def createNeutralUpdate(game: Game, timesAtLocation: Int): Option[Game] = {
    if (timesAtLocation > 3) {
      if (game.isNeutralSite) {
        Some(game.copy(isNeutralSite = false))
      } else {
        None
      }
    } else {
      if (game.isNeutralSite) {
        None
      } else {
        Some(game.copy(isNeutralSite = true))
      }
    }
  }

  override def safeToRun: Future[Boolean] = Future.successful(true)

  override def name: String = "Identify neutral site games"
}
