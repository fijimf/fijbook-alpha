package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Team

import scala.concurrent.Future

trait TeamDAO {

  def deleteTeam(id: Long): Future[Int]

  def findTeamByKey(key: String): Future[Option[Team]]

  def findTeamById(id: Long): Future[Option[Team]]

  def saveTeam(team: Team): Future[Team]

  def saveTeams(teams: List[Team]): Future[List[Team]]

  def listTeams: Future[List[Team]]
}
