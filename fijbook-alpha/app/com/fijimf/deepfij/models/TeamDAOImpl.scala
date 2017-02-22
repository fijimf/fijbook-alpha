package com.fijimf.deepfij.models

import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait TeamDAOImpl extends TeamDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository
  import dbConfig.driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def listTeams:Future[List[Team]] = db.run(repo.teams.to[List].result)

  override def findTeamByKey(key: String): Future[Option[Team]] = db.run(repo.teams.filter(team => team.key === key).result.headOption)

  override def findTeamById(id: Long): Future[Option[Team]] = db.run(repo.teams.filter(team => team.id === id).result.headOption)

  override def saveTeam(team: Team): Future[Team] = {
    db.run(repo.teams.filter(t => t.key === team.key).result.flatMap(ts =>
      ts.headOption match {
        case Some(t) =>
          (repo.teams returning repo.teams.map(_.id)).insertOrUpdate(team.copy(id = t.id))
        case None =>
          (repo.teams returning repo.teams.map(_.id)).insertOrUpdate(team)
      }
    ).flatMap(_=>repo.teams.filter(t => t.key === team.key).result.head).transactionally)
  }

  override def deleteTeam(id: Long): Future[Int] = db.run(repo.teams.filter(team => team.id === id).delete)


}
