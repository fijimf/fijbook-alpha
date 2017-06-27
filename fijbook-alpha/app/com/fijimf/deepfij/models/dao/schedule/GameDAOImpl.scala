package com.fijimf.deepfij.models.dao.schedule

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait GameDAOImpl extends GameDAO with DAOSlick {

  val log:Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]
  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]

  override def listGames: Future[List[Game]] = db.run(repo.games.to[List].result)
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

  override def gamesByDate(ds: List[LocalDate]): Future[List[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.date inSet ds).to[List].result)

  override def gamesBySource(sourceKey: String): Future[List[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.sourceKey === sourceKey).to[List].result)

  override def updateGame(game: Game): Future[Game] = {
    require(game.id>0L, "Cannot update a game where id is unknown")
    db.run(
      repo.games.insertOrUpdate(game).flatMap {
        case 1 => DBIO.successful(game)
        case 0 => DBIO.failed(new RuntimeException("Update unscuccessful"))
        case m => DBIO.failed(new RuntimeException(s"Wrong number of rows affected $m"))
      }
    )
  }

  override def insertGame(game: Game): Future[Game] = {
    db.run(repo.games returning repo.games.map(_.id)+=game).map(i=>game.copy(id=i))
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
