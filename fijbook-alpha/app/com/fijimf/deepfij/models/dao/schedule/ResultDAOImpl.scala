package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Result, ScheduleRepository}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait ResultDAOImpl extends ResultDAO with DAOSlick {

  val log: Logger
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listResults: Future[List[Result]] = db.run(repo.results.to[List].result)

  override def saveResult(result: Result): Future[Result] = saveResults(List(result)).map(_.head)

  override def saveResults(results: List[Result]): Future[List[Result]] = {
    val ops = results.map(r1 => repo.results.filter(r => r.gameId === r1.gameId).result.flatMap(rs =>
      rs.headOption match {
        case Some(t) =>
          (repo.results returning repo.results.map(_.id)).insertOrUpdate(r1.copy(id = t.id))
        case None =>
          (repo.results returning repo.results.map(_.id)).insertOrUpdate(r1)
      }
    ).flatMap(_ => repo.results.filter(t => t.gameId === r1.gameId).result.head))
    db.run(DBIO.sequence(ops).transactionally)
  }

  def deleteResultsByGameId(gameIds: List[Long]): Future[Unit] = {
    if (gameIds.nonEmpty) {
      val deletes: List[DBIOAction[_, NoStream, Write]] = gameIds.map(id => repo.results.filter(_.id === id).delete)

      val action = DBIO.seq(deletes: _*).transactionally
      val future: Future[Unit] = db.run(action)
      future.onComplete((t: Try[Unit]) => {
        t match {
          case Success(_) => log.info(s"Deleted ${gameIds.size} results")
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
