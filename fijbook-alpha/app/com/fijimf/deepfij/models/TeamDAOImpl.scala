package com.fijimf.deepfij.models

import javax.inject.Inject

import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Give access to the user object using Slick
  */
class TeamDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, protected val repo: ScheduleRepository) extends TeamDAO with DAOSlick {
  val log = Logger(getClass)

  import dbConfig.driver.api._


  override def find(key: String) = {
    val q: Query[repo.TeamsTable, Team, Seq] = repo.teams.filter(team => team.key === key)
    db.run(q.result.headOption)
  }

  override def find(id: Long) = {
    val q: Query[repo.TeamsTable, Team, Seq] = repo.teams.filter(team => team.id === id)
    db.run(q.result.headOption)
  }

  override def save(team: Team /*, isAutoUpdate:Boolean */): Future[Int] = {
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

  override def list: Future[List[Team]] = {
    db.run(repo.teams.to[List].result)
  }

  override def unlock(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(false))

  override def lock(key: String): Future[Int] = db.run(repo.teams.filter(t => t.key === key).map(_.lockRecord).update(true))

}