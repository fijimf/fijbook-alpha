package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate
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
    with QuoteDAOImpl
    with SeasonDAOImpl
    with ConferenceDAOImpl
    with AliasDAOImpl
with UserProfileDAOImpl {
  val log = Logger(getClass)

  import dbConfig.driver.api._




  override def listResults: Future[List[Result]] = db.run(repo.results.to[List].result)

  override def listLogisticModel: Future[List[LogisticModelParameter]] = db.run(repo.logisticModels.to[List].result)


  override def listGamePrediction: Future[List[GamePrediction]] = db.run(repo.gamePredictions.to[List].result)

  override def listStatValues: Future[List[StatValue]] = db.run(repo.statValues.to[List].result)


  //******* Season


  //******* Game




  // Conference


  // Schedule

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


  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Unit] = {
    val map: List[DBIOAction[Int, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    db.run(DBIO.seq(map: _*).transactionally)
  }

  override def saveStatValues(batchSize: Int, dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Any] = {
    //    grouped.foreach(d => {
//      Await.result(saveStatBatch(d, models, stats.filter(s => d.contains(s.date))), 3 minutes)
//    })
    def batchReq(ds:List[LocalDate]) = saveStatBatch(ds, models, stats.filter(s => ds.contains(s.date)))
    val g = dates.grouped(batchSize).toList
    g.tail.foldLeft(batchReq(g.head)){case (future: Future[_], dates: List[LocalDate]) => future.flatMap(_=>batchReq(dates))}
  }

  def saveStatBatch(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Any] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.info(s"Saving stat batch for $key (${stats.size} rows)")
    val inserts: DBIOAction[_, NoStream, Write] = repo.statValues ++= stats
    val deletes: List[DBIOAction[_, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    val delete = DBIO.seq(deletes :_*)
    val future: Future[Any] = db.run(delete.andThen(inserts).transactionally)
    future.onComplete((t: Try[_]) => {
      val dur = System.currentTimeMillis() - start
      t match {
        case Success(_) => log.info(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
        case Failure(ex) => log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
      }
    })
    future
  }

  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)

  override def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey && sv.statKey === statKey).to[List].result)

  override def loadStatValues(modelKey: String): Future[List[StatValue]] = db.run(repo.statValues.filter(sv => sv.modelKey === modelKey).to[List].result)

  override def loadGamePredictions(games: List[Game], modelKey: String): Future[List[GamePrediction]] = {
    Future.sequence(games.map(g => {
      db.run(repo.gamePredictions.filter(gp => gp.gameId === g.id && gp.modelKey === modelKey).to[List].result)
    })).map(_.flatten)
  }

  override def saveGamePredictions(gps: List[GamePrediction]): Future[List[Int]] = {
    val saveResults = Future.sequence(gps.map(gp => db.run(repo.gamePredictions.insertOrUpdate(gp))))
    saveResults.onComplete {
      case Success(is) => log.info("Save results: " + is.mkString(","))
      case Failure(ex) => log.error("Failed upserting prediction", ex)
    }
    saveResults
  }

  override def upsertGame(game: Game): Future[Long] = {

    val response = db.run(for (
      id <- (repo.games returning repo.games.map(_.id)).insertOrUpdate(game)
    ) yield id)

    response.onComplete {
      case Success(id) => log.info("Saved game " + id.getOrElse(game.id))
      case Failure(ex) => log.error("Failed upserting game ", ex)
    }
    response.map(_.getOrElse(0L))
  }

  final def runWithRetry[R](a: DBIOAction[R, NoStream, Nothing], n: Int): Future[R] =
    db.run(a).recoverWith {
      case ex: MySQLTransactionRollbackException => {
        if (n == 0) {
          Future.failed(ex)
        } else {
          log.info(s"Caught MySQLTransactionRollbackException.  Retrying ($n)")
          runWithRetry(a, n - 1)
        }
      }
    }

  override def upsertResult(result: Result): Future[Long] = {
    val action = for (
      id <- (repo.results returning repo.results.map(_.id)).insertOrUpdate(result)
    ) yield id
    val r = runWithRetry(action, 10)
    r.onComplete {
      case Success(_) => log.info("Saved result " + result.id + " (" + result.gameId + ")")
      case Failure(ex) => log.error(s"Failed upserting result ${result.id} --> ${result.gameId}", ex)
    }
    r.map(_.getOrElse(0L))
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

  def deleteResults(ids: List[Long]):Future[Unit] = {
    if (ids.nonEmpty) {
      val deletes: List[DBIOAction[_, NoStream, Write]] = ids.map(id => repo.results.filter(_.id === id).delete)

      val action = DBIO.seq(deletes: _*).transactionally
      val future: Future[Unit] = db.run(action)
      future.onComplete((t: Try[Unit]) => {
        t match {
          case Success(_) => log.info(s"Deleted ${ids.size} results")
          case Failure(ex) => log.error(s"Deleting results failed with error: ${ex.getMessage}", ex)
        }
      })
      future
    } else {
      log.info("Delete results called with empty list")
      Future.successful(Unit)
    }
  }


}