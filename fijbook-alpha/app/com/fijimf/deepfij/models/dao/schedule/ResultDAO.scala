package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Result

import scala.concurrent.Future

trait ResultDAO {

  def listResults: Future[List[Result]]

  def deleteResults(ids: List[Long])

  def upsertResult(result: Result): Future[Long]
}
