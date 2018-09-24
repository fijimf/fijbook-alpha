package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{Job, Result}

import scala.concurrent.Future

trait JobDAO {

  def listJobs: Future[List[Job]]

  def saveJob(j: Job): Future[Job]

  def findJobById(id:Long): Future[Option[Job]]

  def findJobByName(name:String): Future[Option[Job]]

  def deleteJob(id:Long): Future[Int]

}
