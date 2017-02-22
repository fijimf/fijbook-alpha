package com.fijimf.deepfij.models
import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait ConferenceDAO {

  def listConferenceMaps: Future[List[ConferenceMap]]

  def saveConferenceMap(cm: ConferenceMap): Future[Int]

  def findConferenceById(id: Long): Future[Option[Conference]]

  def deleteConference(id: Long): Future[Int]

  def listConferences: Future[List[Conference]]

  def saveConference(c: Conference): Future[Int]
}
