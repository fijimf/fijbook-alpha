package com.fijimf.deepfij.models
import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait ResultDAO {

  def listResults: Future[List[Result]]

  def deleteResults(ids: List[Long])

  def upsertResult(result: Result): Future[Long]
}
