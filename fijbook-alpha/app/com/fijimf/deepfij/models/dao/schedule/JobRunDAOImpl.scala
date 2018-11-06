package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, Job, JobRun, Result, ScheduleRepository}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait JobRunDAOImpl extends JobRunDAO with DAOSlick {
  val log: Logger

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listJobRuns: Future[List[JobRun]] = db.run(repo.jobRuns.to[List].result)

  override def saveJobRun(run: JobRun): Future[JobRun] = db.run(upsert(run))

  private def upsert(x: JobRun) = {
    (repo.jobRuns returning repo.jobRuns.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.jobRuns.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  override def findJobRunsByJobId(jobId: Long): Future[List[JobRun]] = db.run(repo.jobRuns.filter(_.jobId===jobId).to[List].result)

  override def deleteJobRun(id: Long): Future[Int] = db.run(repo.jobRuns.filter(_.id===id).delete)

}
