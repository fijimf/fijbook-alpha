package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, RssItem, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait AliasDAOImpl extends AliasDAO with DAOSlick {
  import scala.concurrent.ExecutionContext.Implicits.global
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(alias: Alias): Future[Alias] = db.run(upsert(alias))

  override def saveAliases(aliases: List[Alias]): Future[List[Alias]] = {
    db.run(DBIO.sequence(aliases.map(upsert)).transactionally)
  }

  private def upsert(x: Alias) = {
    (repo.aliases returning repo.aliases.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.aliases.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }

  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)

  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)

}
