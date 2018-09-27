package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.DAOSlick
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait ConferenceDAOImpl extends ConferenceDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listConferences: Future[List[Conference]] = db.run(repo.conferences.to[List].result)

  override def findConferenceById(id: Long): Future[Option[Conference]] = db.run(repo.conferences.filter(_.id === id).result.headOption)

  override def findConferenceByKey(key: String): Future[Option[Conference]] = db.run(repo.conferences.filter(_.key === key).result.headOption)

  override def findConferencesLike(str:String): Future[List[Conference]]= {
    val k = s"%${str.trim}%"
    db.run(
      repo.conferences.filter(conf => conf.name.like(k) || conf.key.like(k)).to[List].result
    )
  }

  override def deleteConference(id: Long): Future[Int] = db.run(repo.conferences.filter(_.id === id).delete)

  override def saveConference(c: Conference): Future[Conference] = db.run(upsert(c))

  override def saveConferences(confs: List[Conference]): Future[List[Conference]] = {
    db.run(DBIO.sequence(confs.map(upsert)).transactionally)
  }


  private def upsert(x: Conference) = {
    (repo.conferences returning repo.conferences.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.conferences.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }

}
