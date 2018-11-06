package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.ConferenceMap

import scala.concurrent.Future

trait ConferenceMapDAO {

  def listConferenceMaps: Future[List[ConferenceMap]]

  def findConferenceMap(seasonId: Long, teamId:Long):Future[Option[ConferenceMap]]

  def deleteConferenceMap(id: Long): Future[Int]

  def deleteConferenceMap(seasonId:Long, conferenceId:Long,teamId:Long): Future[Int]

  def deleteAllConferenceMaps(): Future[Int]

  def saveConferenceMap(cm: ConferenceMap): Future[ConferenceMap]

  def saveConferenceMaps(cms: List[ConferenceMap]): Future[List[ConferenceMap]]

}
