package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.ConferenceMap

import scala.concurrent.Future

trait ConferenceMapDAO {

  def listConferenceMaps: Future[List[ConferenceMap]]

  def saveConferenceMap(cm: ConferenceMap): Future[Int]

}
