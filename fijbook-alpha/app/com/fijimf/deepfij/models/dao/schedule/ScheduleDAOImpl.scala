package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.Effect.Write

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class ScheduleDAOImpl @Inject()(val dbConfigProvider: DatabaseConfigProvider, val repo: ScheduleRepository)
  extends ScheduleDAO with DAOSlick
    with TeamDAOImpl
    with GameDAOImpl
    with ResultDAOImpl
    with QuoteDAOImpl
    with SeasonDAOImpl
    with ConferenceDAOImpl
    with AliasDAOImpl
    with AnalyticsDAOImpl
    with UserProfileDAOImpl {
  //val log = Logger(getClass)

  import dbConfig.driver.api._

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )



  override def listLogisticModel: Future[List[LogisticModelParameter]] = db.run(repo.logisticModels.to[List].result)

  override def listGamePrediction: Future[List[GamePrediction]] = db.run(repo.gamePredictions.to[List].result)

  override def listStatValues: Future[List[StatValue]] = db.run(repo.statValues.to[List].result)

  def loadSchedule(s: Season): Future[Schedule] = {
    val fTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)
    val fConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)
    val fConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.filter(_.seasonId === s.id).to[List].result)
    val fResults: Future[List[(Game, Option[Result])]] = db.run(repo.gameResults.filter(_._1.seasonId === s.id).to[List].result)
    val fPredictions: Future[List[(Game, Option[GamePrediction])]] = db.run(repo.predictedResults.filter(_._1.seasonId === s.id).to[List].result)
    for (
      teams <- fTeams;
      conferences <- fConferences;
      conferenceMap <- fConferenceMaps;
      results <- fResults;
      predictions <- fPredictions
    ) yield {
      Schedule(s, teams, conferences, conferenceMap, results, predictions)
    }
  }

  override def loadSchedules(): Future[List[Schedule]] = {
    db.run(repo.seasons.to[List].result).flatMap(seasons =>
      Future.sequence(seasons.map(season => loadSchedule(season)))
    )
  }

  override def loadLatestSchedule(): Future[Option[Schedule]] = {
    loadSchedules().map(_.sortBy(s => -s.season.year).headOption)
  }

  // Aliases



  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)



  override def upsertGame(game: Game): Future[Long] = {

    val response = db.run(for (
      id <- (repo.games returning repo.games.map(_.id)).insertOrUpdate(game)
    ) yield id)

    response.onComplete {
      case Success(id) => log.trace("Saved game " + id.getOrElse(game.id))
      case Failure(ex) => log.error("Failed upserting game ", ex)
    }
    response.map(_.getOrElse(0L))
  }


  def deleteGames(ids: List[Long]):Future[Unit] = {
    if (ids.nonEmpty) {
      val deletes: List[DBIOAction[_, NoStream, Write]] = ids.map(id => repo.games.filter(_.id === id).delete)

      val action = DBIO.seq(deletes: _*).transactionally
      val future: Future[Unit] = db.run(action)
      future.onComplete((t: Try[Unit]) => {
        t match {
          case Success(_) => log.info(s"Deleted ${ids.size} games")
          case Failure(ex) => log.error(s"Deleting games failed with error: ${ex.getMessage}", ex)
        }
      })
      future
    } else {
      log.info("Delete games called with empty list")
      Future.successful(Unit)
    }
  }



}