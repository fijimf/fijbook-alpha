package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait AliasDAOImpl extends AliasDAO with DAOSlick {
  import scala.concurrent.ExecutionContext.Implicits.global
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(alias: Alias): Future[Alias] = saveAliases(List(alias)).map(_.head)

  override def saveAliases(aliases: List[Alias]): Future[List[Alias]] = {
    val ops = aliases.map(c1 => repo.aliases.filter(c => c.alias === c1.alias).result.flatMap(cs =>
      cs.headOption match {
        case Some(c) =>
          (repo.aliases returning repo.aliases.map(_.id)).insertOrUpdate(c1.copy(id = c.id))
        case None =>
          (repo.aliases returning repo.aliases.map(_.id)).insertOrUpdate(c1)
      }
    ).flatMap(_ => repo.aliases.filter(t => t.alias === c1.alias).result.head))
    db.run(DBIO.sequence(ops).transactionally)
  }


  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)

  override def deleteAlias(id: Long): Future[Int] = db.run(repo.aliases.filter(_.id === id).delete)

}
