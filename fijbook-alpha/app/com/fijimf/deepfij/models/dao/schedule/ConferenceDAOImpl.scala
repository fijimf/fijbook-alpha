package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait ConferenceDAOImpl extends ConferenceDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.driver.api._

  override def listConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.to[List].result)

  override def listConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)

  override def saveConferenceMap(cm: ConferenceMap) = db.run(repo.conferenceMaps.insertOrUpdate(cm))

  override def findConferenceById(id: Long): Future[Option[Conference]] = db.run(repo.conferences.filter(_.id === id).result.headOption)

  override def deleteConference(id: Long): Future[Int] = db.run(repo.conferences.filter(_.id === id).delete)


  override def saveConference(c: Conference): Future[Int] = db.run(repo.conferences.insertOrUpdate(c))

}
