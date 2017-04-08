package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait AliasDAOImpl extends AliasDAO with DAOSlick {
  import scala.concurrent.ExecutionContext.Implicits.global
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(alias: Alias): Future[Alias] =
    db.run(repo.aliases.filter(a => a.key === alias.key).result.flatMap(as =>
      as.headOption match {
        case Some(a) =>
          (repo.aliases returning repo.aliases.map(_.id)).insertOrUpdate(alias.copy(id = a.id))
        case None =>
          (repo.aliases returning repo.aliases.map(_.id)).insertOrUpdate(alias)
      }
    ).flatMap(_=>repo.aliases.filter(t => t.key === alias.key).result.head).transactionally)


  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)

  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)

}
