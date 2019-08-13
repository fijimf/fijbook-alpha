package com.fijimf.deepfij.models.dao.schedule

import akka.actor.ActorSystem
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.schedule.model
import com.fijimf.deepfij.schedule.model.Schedule
import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure


class ScheduleDAOImpl @Inject()(val dbConfigProvider: DatabaseConfigProvider, val repo: ScheduleRepository, val actorSystem: ActorSystem)(implicit ec: ExecutionContext)
  extends ScheduleDAO with DAOSlick
{

  import dbConfig.profile.api._

  val log = Logger(this.getClass)


  def loadSchedule(s: Season): Future[Schedule] = {
    val fTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)
    val fConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)
    val fConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.filter(_.seasonId === s.id).to[List].result)
    val fResults: Future[List[(Game, Option[Result])]] = db.run(repo.gameResults.filter(_._1.seasonId === s.id).to[List].result)
    for {
      teams <- fTeams
      conferences <- fConferences
      conferenceMap <- fConferenceMaps
      results <- fResults

    } yield {
      model.Schedule(s, teams, conferences, conferenceMap, results)
    }
  }
  override def loadSchedules(): Future[List[Schedule]] = {
    val result: DBIO[List[Season]] = repo.seasons.to[List].result
    db.run(result).flatMap(seasons =>
      Future.sequence(seasons.map(season => loadSchedule(season)))
    )
  }

  override def loadSchedule(y: Int): Future[Option[Schedule]] = {
    if (y<0) {
      loadLatestSchedule()
    }
    else {
      val s = System.currentTimeMillis()
      val future = db.run(repo.seasons.filter(_.year === y).result.headOption).flatMap {
        case Some(s) => loadSchedule(s).map(Some(_))
        case None => Future.successful(None)
      }
      future.onComplete {
        case Failure(thr) => log.error(s"loadSchedule for $y failed in ${System.currentTimeMillis() - s} ms. Error was ${thr.getMessage}", thr)
        case _ => log.debug(s"loadSchedule from DB for $y completed in ${System.currentTimeMillis() - s} ms.")
      }
      future
    }
  }

  override def loadLatestSchedule(): Future[Option[Schedule]] = {
    for {
      s <- loadSchedules().map(_.sortBy(s => -s.season.year).headOption)
    } yield {
      s match {
        case Some(sch) => log.info(s"Load latest schedule loaded ${sch.season.year}, with ${sch.incompleteGames.size} games yet to play.")
        case _ => log.warn(s"Failed tp load latest schedule.")
      }
      s
    }
  }

}