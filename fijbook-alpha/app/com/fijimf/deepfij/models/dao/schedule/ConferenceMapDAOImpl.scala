package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait ConferenceMapDAOImpl extends ConferenceMapDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.to[List].result)

  override def deleteConferenceMap(id: Long): Future[Int] = db.run(repo.conferenceMaps.filter(_.id === id).delete)

  override def saveConferenceMap(cm: ConferenceMap): Future[ConferenceMap] = saveConferenceMaps(List(cm)).map(_.head)

  override def saveConferenceMaps(cms: List[ConferenceMap]): Future[List[ConferenceMap]] = {
    val ops = cms.map(c1 => repo.conferenceMaps.filter(cm => cm.conferenceId === c1.conferenceId && cm.seasonId === c1.seasonId).result.flatMap(cs =>
      cs.headOption match {
        case Some(c) =>
          (repo.conferenceMaps returning repo.conferenceMaps.map(_.id)).insertOrUpdate(c1.copy(id = c.id))
        case None =>
          (repo.conferenceMaps returning repo.conferenceMaps.map(_.id)).insertOrUpdate(c1)
      }
    ).flatMap(_ => repo.conferenceMaps.filter(t => t.conferenceId === c1.conferenceId && t.seasonId === c1.seasonId).result.head))
    db.run(DBIO.sequence(ops).transactionally)
  }

}
