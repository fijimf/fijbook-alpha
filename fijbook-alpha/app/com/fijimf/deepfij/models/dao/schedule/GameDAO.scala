package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
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

  def gamesById(id: Long): Future[Option[(Game, Option[Result])]] =
    db.run(repo.gameResults.filter(_._1.id === id).to[List].result.headOption)

  def updateGame(g: Game): Future[Game] = db.run(upsert(g))

  def updateGames(gs: List[Game]): Future[List[Game]] = db.run(DBIO.sequence(gs.map(upsert)).transactionally)

  def updateGameWithResult(g: Game, r: Option[Result]): Future[(Game, Option[Result])] = r match {
    case Some(res) => db.run(upsertGameAndResult(g, res).transactionally).map { case (gg, rr) => (gg, Some(rr)) }
    case None => db.run(upsert(g)).map(g1 => (g1, Option.empty[Result]))
  }

  def updateGamesWithResults(gs: List[(Game, Option[Result])]): Future[List[(Game, Option[Result])]] = {
    db.run(DBIO.sequence(gs.map {
      case (g, Some(r)) => upsertGameAndResult(g, r).map(t => (t._1, Some(t._2)))
      case (g, None) =>
        for {
          g1 <- upsert(g)
          _ <- repo.results.filter(_.gameId === g1.id).delete
        } yield {
          (g1, None)
        }
    }).transactionally)
  }

  private def upsert(x: Game) = {
    require(! (x.awayTeamId===x.homeTeamId),"Home and away teams are the same")
    (repo.games returning repo.games.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.games.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }

  private def upsertResult(r: Result) = {
    (repo.results returning repo.results.map(_.id)).insertOrUpdate(r).flatMap {
      case Some(id) => repo.results.filter(_.id === id).result.head
      case None => DBIO.successful(r)
    }
  }

  private def upsertGameAndResult(g: Game, r: Result) = {
    for {
      g1 <- upsert(g)
      r1 <- upsertResult(r.copy(gameId = g1.id))
    } yield {
      (g1, r1)
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
