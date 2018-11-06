package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{Job, JobRun, Result}

import scala.concurrent.Future

trait JobRunDAO {

  def listJobRuns: Future[List[JobRun]]

  def saveJobRun(run: JobRun): Future[JobRun]

  def findJobRunsByJobId(jobId:Long): Future[List[JobRun]]

  def deleteJobRun(id:Long): Future[Int]

}
