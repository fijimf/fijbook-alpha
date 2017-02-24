package com.fijimf.deepfij.models

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait AliasDAOImpl extends AliasDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  override def deleteAliases(): Future[Int] = db.run(repo.aliases.delete)

  override def findAliasById(id: Long): Future[Option[Alias]] = db.run(repo.aliases.filter(_.id === id).result.headOption)

  override def saveAlias(a: Alias): Future[Int] = db.run(repo.aliases.insertOrUpdate(a))

  override def listAliases: Future[List[Alias]] = db.run(repo.aliases.to[List].result)

}
