package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{Conference, ConferenceMap}

import scala.concurrent.Future

trait ConferenceDAO {

  def listConferenceMaps: Future[List[ConferenceMap]]

  def saveConferenceMap(cm: ConferenceMap): Future[Int]

  def findConferenceById(id: Long): Future[Option[Conference]]

  def deleteConference(id: Long): Future[Int]

  def listConferences: Future[List[Conference]]

  def saveConference(c: Conference): Future[Int]
}
