package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
import cats.implicits._
import scala.util.{Failure, Success, Try}


trait GameDAO extends DAOSlick {

  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]
  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  def listGames: Future[List[Game]] = db.run(repo.games.to[List].result)

  def saveGame(gt: (Game, Option[Result])): Future[Long] = {
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

  def saveGames(gts: List[(Game, Option[Result])]): Future[List[Long]] = {
    db.run(DBIO.sequence(gts.map(gt => {
      val (game, optResult) = gt


      optResult match {
        case Some(result) =>
          for (
            qid <- (repo.games returning repo.games.map(_.id)) += game;
            _ <- repo.results returning repo.results.map(_.id) += result.copy(gameId = qid)
          ) yield qid
        case None =>
          (for (
            qid <- (repo.games returning repo.games.map(_.id)) += game
          ) yield qid).transactionally
      }
    })).transactionally)
  }

  def gamesById(id: Long): Future[Option[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.id === id).to[List].result.headOption)

  def updateGame(g: Game): Future[Game] = db.run(upsert(g))

  def updateGames(gs: List[Game]): Future[List[Game]] = db.run(DBIO.sequence(gs.map(upsert)).transactionally)

  private def upsert(x: Game) = {
    require(! (x.awayTeamId===x.homeTeamId),"Home and away teams are the same")
    (repo.games returning repo.games.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.games.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }

  def deleteGames(ids: List[Long]): Future[Unit] = {
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
