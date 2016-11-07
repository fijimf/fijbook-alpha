package com.fijimf.deepfij.models

import com.sun.org.apache.xpath.internal.operations.Gt

import scala.concurrent.Future

/**
  * Give access to the user object.
  */
trait ScheduleDAO {
  def listAliases: Future[List[Alias]]

  def saveGame(gt: (Game, Option[Result])):Future[Long]

  def saveQuote(q: Qotd): Future[Int]


  def findTeamByKey(key: String): Future[Option[Team]]

  def findTeamById(id: Long): Future[Option[Team]]

  def saveTeam(team: Team): Future[Int]
  def saveSeason(season: Season): Future[Int]
  def findSeasonById(id:Long): Future[Option[Season]]

  def unlockTeam(key: String): Future[Int]

  def lockTeam(key: String): Future[Int]

  def listTeams: Future[List[Team]]
}