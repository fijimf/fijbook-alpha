package com.fijimf.deepfij.models

import java.time.LocalDate

import scala.concurrent.Future

/**
  * Give access to the user object.
  */
trait ScheduleDAO {
  def unlockSeason(seasonId: Long): Future[Int]

  def checkAndSetLock(seasonId: Long): Boolean

  def saveAlias(a: Alias): Future[Int]

  def findAliasById(id: Long): Future[Option[Alias]]

  def listAliases: Future[List[Alias]]

  def deleteAlias(id: Long): Future[Int]

  def clearGamesByDate(d:LocalDate):Future[Int]

  def saveGame(gt: (Game, Option[Result])): Future[Long]

  def saveQuote(q: Quote): Future[Int]

  def findTeamByKey(key: String): Future[Option[Team]]

  def findTeamById(id: Long): Future[Option[Team]]

  def saveTeam(team: Team): Future[Int]

  def saveSeason(season: Season): Future[Int]

  def findSeasonById(id: Long): Future[Option[Season]]

  def unlockTeam(key: String): Future[Int]

  def lockTeam(key: String): Future[Int]

  def listTeams: Future[List[Team]]

  def listQuotes: Future[List[Quote]]

  def findQuoteById(id: Long): Future[Option[Quote]]

  def deleteQuote(id: Long): Future[Int]

  def loadSchedules(): Future[List[Schedule]]
}