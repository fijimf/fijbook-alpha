package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Conference

import scala.concurrent.Future

trait ConferenceDAO {

  def findConferenceById(id: Long): Future[Option[Conference]]

  def deleteConference(id: Long): Future[Int]

  def listConferences: Future[List[Conference]]

  def saveConference(c: Conference): Future[Conference]

  def saveConferences(c: List[Conference]): Future[List[Conference]]
}
