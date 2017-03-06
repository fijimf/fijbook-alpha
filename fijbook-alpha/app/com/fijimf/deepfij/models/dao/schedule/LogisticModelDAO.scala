package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._

import scala.concurrent.Future

trait LogisticModelDAO {

  def listLogisticModel: Future[List[LogisticModelParameter]]

  def deleteLogisticModels(): Future[Int]

  def saveLogisticModel(lm: LogisticModelParameter): Future[Int]

  def findLogisticModelById(id: Long): Future[Option[Alias]]

  def deleteLogisticModel(id: Long): Future[Int]

}
