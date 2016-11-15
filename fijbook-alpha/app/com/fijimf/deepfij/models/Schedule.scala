package com.fijimf.deepfij.models

import java.time.LocalDate

case class Schedule(season:Season, teams:List[Team], conferences:List[Conference], conferenceMap:List[ConferenceMap], gameResults:List[(Game,Option[Result])]) {
  val games = gameResults.map(_._1).sortBy(_.date.toLocalDate.toEpochDay)
  val gameMap = games.map(g=>g.id->g).toMap
  val results = gameResults.filter(_._2.isDefined).sortBy(_._1.date.toLocalDate.toEpochDay).map(_._2.get)
  val teamConference= conferenceMap.map(c=>c.teamId->c.conferenceId).toMap
  def firstGame:Option[LocalDate] = games.headOption.map(_.date.toLocalDate)
  def lastGame:Option[LocalDate] = games.lastOption.map(_.date.toLocalDate)
  def numResults:Int = games.size
  def firstResult:Option[LocalDate] = results.headOption.map(r=> gameMap(r.gameId).date.toLocalDate)
  def lastResult:Option[LocalDate] = results.lastOption.map(r=> gameMap(r.gameId).date.toLocalDate)
  def teamsMapped = teams.filter(t=>teamConference.contains(t.id))
}
