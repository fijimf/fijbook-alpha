package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait ConferenceMapDAOImpl extends ConferenceMapDAO with DAOSlick {
  val logger = Logger(getClass)

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listConferenceMaps: Future[List[ConferenceMap]] = db.run(repo.conferenceMaps.to[List].result)

  override def deleteConferenceMap(id: Long): Future[Int] = db.run(repo.conferenceMaps.filter(_.id === id).delete)

  override def deleteAllConferenceMaps(): Future[Int] = {
    val f = db.run(repo.conferenceMaps.delete)
    f.onComplete {
      case Success(i)=> logger.info(s"Deleted $i conference mappings")
      case Failure(ex) =>logger.error(s"Filed deleting conference maps",ex)
    }
    f
  }

  override def saveConferenceMap(cm: ConferenceMap): Future[ConferenceMap] = saveConferenceMaps(List(cm)).map(_.head)

  override def saveConferenceMaps(cms: List[ConferenceMap]): Future[List[ConferenceMap]] = {
    logger.info(s"Saving a list of ${cms.size} conference mappings")
    val ops = cms.map(c1 => repo.conferenceMaps.
      filter(cm =>
        cm.conferenceId === c1.conferenceId && cm.seasonId === c1.seasonId && cm.teamId === c1.teamId
      ).result.flatMap(cs =>
      cs.headOption match {
        case Some(c) =>
          (repo.conferenceMaps returning repo.conferenceMaps.map(_.id)).insertOrUpdate(c1.copy(id = c.id))
        case None =>
          (repo.conferenceMaps returning repo.conferenceMaps.map(_.id)).insertOrUpdate(c1)
      }
    ).flatMap(_ => repo.conferenceMaps.
      filter(t =>
        t.conferenceId === c1.conferenceId && t.seasonId === c1.seasonId && t.teamId === c1.teamId).result.head))
    val f = db.run(DBIO.sequence(ops).transactionally)
    f.onComplete {
      case Success(lcm)=> logger.info(s"Saved ${lcm.size} conference mappings")
      case Failure(ex) =>logger.error(s"Failed saving conference maps",ex)
    }
    f

  }

}
