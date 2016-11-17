package com.fijimf.deepfij.models

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class ScheduleDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val repo: ScheduleRepository) extends ScheduleDAO with DAOSlick {
  val log = Logger(getClass)

  import dbConfig.driver.api._

  override def listTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)

  override def findTeamByKey(key: String) = db.run(repo.teams.filter(team => team.key === key).result.headOption)

  override def findTeamById(id: Long) = db.run(repo.teams.filter(team => team.id === id).result.headOption)

  override def saveTeam(team: Team /*, isAutoUpdate:Boolean */): Future[Int] = {
    log.info("Saving team " + team.key)

    val rowsAffected: Future[Int] = db.run(repo.teams.filter(t => t.key === team.key).result.flatMap(ts =>
      ts.headOption match {
        case Some(t) => {
          if (!t.lockRecord) repo.teams.insertOrUpdate(team.copy(id = t.id)) else repo.teams.filter(z => z.id === -1L).update(team)
        }
        case None => {
          repo.teams.insertOrUpdate(team)
        }
      }
    ).transactionally)

    rowsAffected.onComplete {
      case Success(i) => log.info(team.key + " save succeeded")
      case Failure(thr) => log.error("Failed saving " + team.toString, thr)
    }
    rowsAffected
  }

  override def unlockTeam(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(false))

  override def lockTeam(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(true))

  override def saveSeason(s: Season): Future[Int] = db.run(repo.seasons.insertOrUpdate(s))

  override def findSeasonById(id: Long): Future[Option[Season]] = {
    val q: Query[repo.SeasonsTable, Season, Seq] = repo.seasons.filter(season => season.id === id)
    db.run(q.result.headOption)
  }

  override def saveGame(gt: (Game, Option[Result])) = {
    val (game, optResult) = gt

    optResult match {
      case Some(result) =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game;
          rid <- repo.results returning repo.results.map(_.id) += result.copy(gameId = qid)
        ) yield qid).transactionally)
      case None =>
        db.run((for (
          qid <- (repo.games returning repo.games.map(_.id)) += game
        ) yield qid).transactionally)
    }
  }


  override def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  override def findQuoteById(id: Long): Future[Option[Quote]] = {
    db.run(repo.quotes.filter(_.id === id).result.headOption)
  }

  override def saveQuote(q: Quote) = db.run(repo.quotes.insertOrUpdate(q))

  override def deleteQuote(id: Long):Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

  def loadSchedule(s: Season):Future[Schedule] = {
    val fTeams: Future[List[Team]] = db.run(repo.teams.to[List].result)
    val fConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)
    val fConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.filter(_.seasonId===s.id).to[List].result)
    val fResults: Future[List[(Game,Option[Result])]] = db.run(repo.gameResults.filter(_._1.seasonId===s.id).to[List].result)
    for(
      teams<-fTeams;
      conferences<-fConferences;
      conferenceMap<-fConferenceMaps;
      results<-fResults
    ) yield {
      Schedule(s,teams,conferences,conferenceMap, results)
    }
  }

  override def loadSchedules(): Future[List[Schedule]] = {
    db.run(repo.seasons.to[List].result).flatMap(seasons=>
      Future.sequence(seasons.map(season=>loadSchedule(season)))
    )
  }


  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)


  override def findAliasById(id: Long): Future[Option[Alias]] = {
    db.run(repo.aliases.filter(_.id === id).result.headOption)
  }

  override def unlockSeason(seasonId: Long): Future[Int] = db.run(repo.seasons.filter(s=> s.id===seasonId).map(_.lock).update("open"))

  override def checkAndSetLock(seasonId: Long): Boolean = { // Note this function blocks
    val run: Future[Int] = db.run(repo.seasons.filter(s=> s.id===seasonId && s.lock=!="lock" && s.lock=!="update").map(_.lock).update("update"))
    val map: Future[Boolean] = run.map(n => n == 1)
    val result: Boolean = Await.result(map, Duration(15,TimeUnit.SECONDS) )
result
  }

  override def saveAlias(a: Alias) = db.run(repo.aliases.insertOrUpdate(a))

  override def deleteAlias(id: Long):Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)
}