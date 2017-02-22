package com.fijimf.deepfij.models
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait TeamDAO {

  def deleteTeam(id: Long): Future[Int]

  def findTeamByKey(key: String): Future[Option[Team]]

  def findTeamById(id: Long): Future[Option[Team]]

  def saveTeam(team: Team): Future[Team]

  def listTeams: Future[List[Team]]
}
