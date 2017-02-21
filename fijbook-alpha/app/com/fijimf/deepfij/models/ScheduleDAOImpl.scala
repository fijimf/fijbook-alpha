package com.fijimf.deepfij.models

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.dbio.Effect.Write

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class ScheduleDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val repo: ScheduleRepository) extends ScheduleDAO with DAOSlick {
  val log = Logger(getClass)

  import dbConfig.driver.api._


  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE_TIME),
    str => LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(str))
  )
  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate] = MappedColumnType.base[LocalDate, String](
    ldt => ldt.format(DateTimeFormatter.ISO_DATE),
    str => LocalDate.from(DateTimeFormatter.ISO_DATE.parse(str))
  )

  override def listConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.to[List].result)

  override def listTeams:Future[List[Team]] = db.run(repo.teams.to[List].result)

  override def listResults: Future[List[Result]] = db.run(repo.results.to[List].result)

  override def listLogisticModel: Future[List[LogisticModelParameter]] = db.run(repo.logisticModels.to[List].result)

  override def listGames: Future[List[Game]] = db.run(repo.games.to[List].result)

  override def listGamePrediction: Future[List[GamePrediction]] = db.run(repo.gamePredictions.to[List].result)

  override def listStatValues: Future[List[StatValue]] = db.run(repo.statValues.to[List].result)

  override def listSeasons: Future[List[Season]] = db.run(repo.seasons.to[List].result)

  override def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  override def listConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)

  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)


  override def findTeamByKey(key: String): Future[Option[Team]] = db.run(repo.teams.filter(team => team.key === key).result.headOption)

  override def findTeamById(id: Long): Future[Option[Team]] = db.run(repo.teams.filter(team => team.id === id).result.headOption)

  override def saveTeam(team: Team): Future[Team] = {
    db.run(repo.teams.filter(t => t.key === team.key).result.flatMap(ts =>
      ts.headOption match {
        case Some(t) =>
          (repo.teams returning repo.teams.map(_.id)).insertOrUpdate(team.copy(id = t.id))
        case None =>
          (repo.teams returning repo.teams.map(_.id)).insertOrUpdate(team)
      }
    ).flatMap(_=>repo.teams.filter(t => t.key === team.key).result.head).transactionally)
  }

  override def deleteTeam(id: Long): Future[Int] = db.run(repo.teams.filter(team => team.id === id).delete)

  //******* Season

  override def saveSeason(s: Season): Future[Int] = db.run(repo.seasons.insertOrUpdate(s))

  override def findSeasonById(id: Long): Future[Option[Season]] = {
    val q = repo.seasons.filter(season => season.id === id)
    db.run(q.result.headOption)
  }


  override def unlockSeason(seasonId: Long): Future[Int] = {
    log.info("Unlocking season " + seasonId)
    db.run(repo.seasons.filter(s => s.id === seasonId).map(_.lock).update("open"))
  }

  override def checkAndSetLock(seasonId: Long): Boolean = {
    // Note this function blocks
    val run: Future[Int] = db.run(repo.seasons.filter(s => s.id === seasonId && s.lock =!= "lock" && s.lock =!= "update").map(_.lock).update("update"))
    val map: Future[Boolean] = run.map(n => n == 1)
    log.info("Locking season " + seasonId + " for update")
    Await.result(map, Duration(15, TimeUnit.SECONDS))
  }

  //******* Game

  override def clearGamesByDate(d: LocalDate): Future[Int] = {
    val dateGames = repo.games.filter(g => g.date === d)
    val dateResults = repo.results.filter(_.gameId in dateGames.map(_.id))
    db.run((dateResults.delete andThen dateGames.delete).transactionally)
  }

  override def saveGame(gt: (Game, Option[Result])): Future[Long] = {
    val (game, optResult) = gt

    optResult match {
      case Some(result) =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game;
          _ <- repo.results returning repo.results.map(_.id) += result.copy(gameId = qid)
        ) yield qid).transactionally)
      case None =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game
        ) yield qid).transactionally)
    }
  }

  override def gamesByDate(ds:List[LocalDate]):Future[List[(Game,Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.date inSet ds).to[List].result)
 override def gamesBySource(sourceKey:String):Future[List[(Game,Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.sourceKey === sourceKey).to[List].result)


  // Quote



  override def findQuoteById(id: Long): Future[Option[Quote]] = db.run(repo.quotes.filter(_.id === id).result.headOption)

  override def findQuoteByKey(key: Option[String]): Future[List[Quote]] = db.run(repo.quotes.filter(_.key === key).to[List].result)

  override def saveQuote(q: Quote): Future[Int] = db.run(repo.quotes.insertOrUpdate(q))

  override def deleteQuote(id: Long): Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

  // Conference

  override def findSeasonByYear(year: Int) = db.run(repo.seasons.filter(_.year === year).result.headOption)

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def saveConferenceMap(cm: ConferenceMap) = db.run(repo.conferenceMaps.insertOrUpdate(cm))

  override def findConferenceById(id: Long): Future[Option[Conference]] = db.run(repo.conferences.filter(_.id === id).result.headOption)

  override def deleteConference(id: Long): Future[Int] = db.run(repo.conferences.filter(_.id === id).delete)


  override def saveConference(c: Conference): Future[Int] = db.run(repo.conferences.insertOrUpdate(c))


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
   loadSchedules().map(_.sortBy(s=> -s.season.year).headOption)
  }

  // Aliases


  override def deleteStatValues(dates: List[LocalDate], models: List[String]): Future[Unit] = {
    val map: List[DBIOAction[Int, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    db.run(DBIO.seq(map: _*).transactionally)
  }

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(a: Alias): Future[Int] = db.run(repo.aliases.insertOrUpdate(a))

  override def saveStatValues(batchSize: Int, dates: List[LocalDate], models: List[String], stats: List[StatValue]): Unit = {
    val grouped = dates.grouped(batchSize)
    grouped.foreach(d => {
      Await.result(saveStatBatch(d, models, stats.filter(s => d.contains(s.date))),3 minutes)
    })

  }

  def saveStatBatch(dates: List[LocalDate], models: List[String], stats: List[StatValue]): Future[Unit] = {
    val key = s"[${models.mkString(", ")}] x [${dates.head} .. ${dates.last}] "
    val start = System.currentTimeMillis()
    log.info(s"Saving stat batch for $key (${stats.size} rows)")
    val inserts = List(repo.statValues.forceInsertAll(stats))
    val deletes: List[DBIOAction[_, NoStream, Write]] =
      for (m <- models; d <- dates) yield {
        repo.statValues.filter(sv => sv.date === d && sv.modelKey === m).delete
      }
    val action = DBIO.seq(deletes ::: inserts: _*).transactionally
    val future: Future[Unit] = db.run(action)
    future.onComplete((t: Try[Unit]) =>{
      val dur = System.currentTimeMillis()-start
      t match {

      case Success(_)=>log.info(s"Completed saving $key in $dur ms. (${1000 * stats.size / dur} rows/sec)")
      case Failure(ex)=>log.error(s"Saving $key failed with error: ${ex.getMessage}", ex)
    }})
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
  override def saveGamePredictions(gps:List[GamePrediction]): Future[List[Int]] = {
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
      case Success(id) => log.info("Saved game " +id.getOrElse(game.id))
      case Failure(ex) => log.error("Failed upserting game ", ex)
    }
    response.map(_.getOrElse(0L))
  }

  final def runWithRetry[R](a: DBIOAction[R, NoStream, Nothing], n:Int): Future[R] =
    db.run(a).recoverWith{
      case ex:MySQLTransactionRollbackException=> {
        if (n==0) {
          Future.failed(ex)
        } else {
          log.info(s"Caught MySQLTransactionRollbackException.  Retrying ($n)")
          runWithRetry(a, n - 1)
        }
      }
    }

  override def upsertResult(result:Result) :Future[Long] = {
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

  def deleteGames(ids:List[Long]) = {
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
    } else {
      log.info("Delete games called with empty list")
    }
  }
  def deleteResults(ids:List[Long]) = {
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
    } else {
      log.info("Delete results called with empty list")
    }
  }


}