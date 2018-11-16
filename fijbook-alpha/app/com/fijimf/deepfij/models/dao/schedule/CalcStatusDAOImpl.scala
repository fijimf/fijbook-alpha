package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, CalcStatus, FavoriteLink, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait CalcStatusDAOImpl extends CalcStatusDAO with DAOSlick {
  import scala.concurrent.ExecutionContext.Implicits.global
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  override def findStatus(seasonId:Long, modelKey:String): Future[Option[CalcStatus]] =
    db.run(repo.calcStatuses.filter(c=>c.seasonId === seasonId && c.modelKey == modelKey).result.headOption)

  override def saveStatus(stat: CalcStatus): Future[CalcStatus] = db.run(upsert(stat))

  private def upsert(stat: CalcStatus) = {
    (repo.calcStatuses returning repo.calcStatuses.map(_.id)).insertOrUpdate(stat).flatMap {
      case Some(id) => repo.calcStatuses.filter(_.id === id).result.head
      case None => DBIO.successful(stat)
    }
  }

  override def deleteStatus(id: Long): Future[Int] =  db.run(repo.calcStatuses.filter(_.id === id).delete)

}
