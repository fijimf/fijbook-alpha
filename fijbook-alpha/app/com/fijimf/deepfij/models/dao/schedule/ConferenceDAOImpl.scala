package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait ConferenceDAOImpl extends ConferenceDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)

  override def findConferenceById(id: Long): Future[Option[Conference]] = db.run(repo.conferences.filter(_.id === id).result.headOption)

  override def findConferenceByKey(key: String): Future[Option[Conference]] = db.run(repo.conferences.filter(_.key === key).result.headOption)

  override def deleteConference(id: Long): Future[Int] = db.run(repo.conferences.filter(_.id === id).delete)

  override def saveConference(c: Conference): Future[Conference] = saveConferences(List(c)).map(_.head)

  override def saveConferences(confs: List[Conference]): Future[List[Conference]] = {
    val ops = confs.map(c1 => repo.conferences.filter(c => c.key === c1.key).result.flatMap(cs =>
      cs.headOption match {
        case Some(c) =>
          (repo.conferences returning repo.conferences.map(_.id)).insertOrUpdate(c1.copy(id = c.id))
        case None =>
          (repo.conferences returning repo.conferences.map(_.id)).insertOrUpdate(c1)
      }
    ).flatMap(_ => repo.conferences.filter(t => t.key === c1.key).result.head))
    db.run(DBIO.sequence(ops).transactionally)
  }

}
