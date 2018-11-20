package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Job, ScheduleRepository}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait JobDAOImpl extends JobDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  def listJobs: Future[List[Job]] = db.run(repo.jobs.to[List].result)

  def saveJob(j: Job): Future[Job] = {
    log.info(j.toString)
    db.run(upsert(j))
  }

  private def upsert(x: Job): DBIOAction[Job, NoStream, Effect.Write with Effect.Read] = {
    (repo.jobs returning repo.jobs.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.jobs.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  def findJobById(id: Long): Future[Option[Job]] = db.run(repo.jobs.filter(_.id === id).result.headOption)
  def findJobByName(name: String): Future[Option[Job]] = db.run(repo.jobs.filter(_.name === name).result.headOption)

  def deleteJob(id: Long): Future[Int] = db.run(repo.jobs.filter(_.id === id).delete)

}
