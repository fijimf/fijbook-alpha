package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Result

import scala.concurrent.Future

trait ResultDAO {

  def listResults: Future[List[Result]]

  def saveResult(result: Result): Future[Result]

  def saveResults(results: List[Result]): Future[List[Result]]

  def deleteResultsByGameId(gameIds: List[Long]): Future[Unit]

}
