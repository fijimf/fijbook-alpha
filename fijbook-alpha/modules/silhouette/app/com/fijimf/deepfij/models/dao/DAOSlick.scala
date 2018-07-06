package com.fijimf.deepfij.models.dao

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.after
import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException
import play.api.Logger
import play.api.db.slick.HasDatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

/**
  * Trait that contains generic slick db handling code to be mixed in with DAOs
  */
trait DAOSlick extends HasDatabaseConfigProvider[JdbcProfile] {
  def log: Logger

  def actorSystem: ActorSystem

  val backoffStrategy = List(
    50.milliseconds,
    50.milliseconds,
    100.milliseconds,
    150.milliseconds,
    200.milliseconds,
    400.milliseconds,
    450.milliseconds,
    500.milliseconds,
    750.milliseconds,
    1000.millisecond,
    1100.milliseconds,
    1200.milliseconds,
    1300.milliseconds,
    1325.milliseconds,
    1500.milliseconds,
    2.second,
    3.second,
    4.second,
    5.second,
    6.second,
    7.second,
    9.second,
    10.seconds,
    12.seconds,
    15.seconds,
    18.seconds
  )

  def runWithRecover[R]
  (
    updateAndCleanUp: DBIO[R],
    ds: List[FiniteDuration]
  )(
    implicit s: Scheduler=actorSystem.scheduler
  ): Future[R] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Random.shuffle(ds).headOption match {
      case Some(d) =>
        val delay = d+ Random.nextInt(250).milliseconds
        db.run(updateAndCleanUp).recoverWith {
          case rollback:MySQLTransactionRollbackException => {
            log.info(s"${rollback.getMessage}  Retrying in $delay.  (${ds.size-1} tries left")
            after(delay, s) {
              runWithRecover(updateAndCleanUp, ds.tail)
            }
          }
          case thr=> {
            log.info(s"${thr.getMessage}  Retrying once in $delay.")
            runWithRecover(updateAndCleanUp, List.empty)
          }
        }
      case None =>
        db.run(updateAndCleanUp)
    }
  }

}