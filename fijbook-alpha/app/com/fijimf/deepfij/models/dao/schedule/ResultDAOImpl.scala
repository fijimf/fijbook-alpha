package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Quote, Result, ScheduleRepository}
import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.Write

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


trait ResultDAOImpl extends ResultDAO with DAOSlick {

  val log:Logger
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

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

  override def listResults: Future[List[Result]] = db.run(repo.results.to[List].result)


  def deleteResultsByGameId(gameIds: List[Long]):Future[Unit] = {
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

  override def upsertResult(result: Result): Future[Long] = {
    val action = for (
      id <- (repo.results returning repo.results.map(_.id)).insertOrUpdate(result)
    ) yield id
    val r = runWithRetry(action, 10)
    r.onComplete {
      case Success(_) => log.trace("Saved result " + result.id + " (" + result.gameId + ")")
      case Failure(ex) => log.error(s"Failed upserting result ${result.id} --> ${result.gameId}", ex)
    }
    r.map(_.getOrElse(0L))
  }


}
