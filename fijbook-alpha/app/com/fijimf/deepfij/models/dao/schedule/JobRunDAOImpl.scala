package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Job, JobRun, Result, ScheduleRepository}
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

  override def saveJobRun(run: JobRun): Future[JobRun] = db.run(
    (repo.jobRuns returning repo.jobRuns.map(_.id)).insertOrUpdate(run)
      .flatMap(i => {
        repo.jobRuns.filter(js => js.id === i.getOrElse(run.id)).result.headOption
      })
  ).map(_.getOrElse(run))


  override def findJobRunsByJobId(jobId: Long): Future[List[JobRun]] = db.run(repo.jobRuns.filter(_.jobId===jobId).to[List].result)

  override def deleteJobRun(id: Long): Future[Int] = db.run(repo.jobRuns.filter(_.id===id).delete)

}
