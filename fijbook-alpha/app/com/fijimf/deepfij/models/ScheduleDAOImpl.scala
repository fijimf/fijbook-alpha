package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
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

    db.run(optResult match {
      case Some(result) =>
        (for (
          qid <- (repo.games returning repo.games.map(_.id)) += game;
          rid <- repo.results returning repo.results.map(_.id) += result.copy(gameId = qid)
        ) yield qid).transactionally
      case None =>
        (for (
          qid <- (repo.games returning repo.games.map(_.id)) += game
        ) yield qid).transactionally
    })
  }


  override def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  override def findQuoteById(id: Long): Future[Option[Quote]] = {
    db.run(repo.quotes.filter(_.id === id).result.headOption)
  }

  override def saveQuote(q: Quote) = db.run(repo.quotes.insertOrUpdate(q))

  override def deleteQuote(id: Long):Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)
}