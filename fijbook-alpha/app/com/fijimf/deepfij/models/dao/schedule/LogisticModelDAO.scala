package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models._

import scala.concurrent.Future

trait LogisticModelDAO {

  def listLogisticModelParameters: Future[List[LogisticModelParameter]]

  def saveLogisticModelParameter(lm: LogisticModelParameter): Future[LogisticModelParameter]

  def findLogisticModel(model: String): Future[Map[LocalDate, List[LogisticModelParameter]]]

  def findLogisticModelDate(model: String, asOf: LocalDate): Future[List[LogisticModelParameter]]

  def findLatestLogisticModel(model: String): Future[List[LogisticModelParameter]]

  def deleteLogisticModel(model: String): Future[Int]

  def deleteLogisticModelDate(model: String, asOf: LocalDate): Future[Int]

}
