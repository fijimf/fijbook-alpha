package com.fijimf.deepfij.models

import java.time.LocalDate

import scala.concurrent.Future

trait ScheduleDAO {
  def findSeasonByYear(year:Int):Future[Option[Season]]

  def deleteAliases():Future[Int]

  def saveConferenceMap(cm: ConferenceMap):Future[Int]

  def findConferenceById(id: Long): Future[Option[Conference]]

  def deleteConference(id: Long): Future[Int]

  def listConferences: Future[List[Conference]]

  def saveConference(c: Conference): Future[Int]

  def unlockSeason(seasonId: Long): Future[Int]

  def checkAndSetLock(seasonId: Long): Boolean

  def saveAlias(a: Alias): Future[Int]

  def findAliasById(id: Long): Future[Option[Alias]]

  def listAliases: Future[List[Alias]]

  def deleteStatValues(dates:List[LocalDate], model:List[String]): Future[Unit]

  def deleteTeam(id: Long): Future[Int]

  def clearGamesByDate(d: LocalDate): Future[Int]

  def saveGame(gt: (Game, Option[Result])): Future[Long]

  def saveQuote(q: Quote): Future[Int]

  def findTeamByKey(key: String): Future[Option[Team]]

  def findTeamById(id: Long): Future[Option[Team]]

  def saveTeam(team: Team): Future[Int]

  def saveSeason(season: Season): Future[Int]

  def findSeasonById(id: Long): Future[Option[Season]]
  def listSeasons: Future[List[Season]]

  def unlockTeam(key: String): Future[Int]

  def lockTeam(key: String): Future[Int]

  def listTeams: Future[List[Team]]

  def listQuotes: Future[List[Quote]]

  def findQuoteById(id: Long): Future[Option[Quote]]

  def findQuoteByKey(key:Option[String]): Future[List[Quote]]

  def deleteQuote(id: Long): Future[Int]

  def loadSchedules(): Future[List[Schedule]]

  def saveStatValues(dates:List[LocalDate], model:List[String], stats:List[StatValue]):Future[Unit]

  def deleteAlias(id: Long): Future[Int]

  def loadStatValues(statKey:String, modelKey:String):Future[List[StatValue]]
  def loadStatValues(modelKey:String):Future[List[StatValue]]
}