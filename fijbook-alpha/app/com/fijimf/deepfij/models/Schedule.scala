package com.fijimf.deepfij.models

import java.time.LocalDate

case class Schedule(season: Season, teams: List[Team], conferences: List[Conference], conferenceMap: List[ConferenceMap], gameResults: List[(Game, Option[Result])]) {
  val games: List[Game] = gameResults.map(_._1).sortBy(_.date.toEpochDay)
  val gameMap: Map[Long, Game] = games.map(g => g.id -> g).toMap
  val results: List[Result] = gameResults.filter(_._2.isDefined).sortBy(_._1.date.toEpochDay).map(_._2.get)
  val resultMap: Map[Long, Result] = results.map(r => r.gameId -> r).toMap
  val conferenceIdMap: Map[Long, Conference] = conferences.map(r => r.id -> r).toMap
  val teamConference: Map[Long, Long] = conferenceMap.map(c => c.teamId -> c.conferenceId).toMap
  val conferenceTeams: Map[Long, List[Long]] = conferenceMap.groupBy(_.conferenceId).map(c => c._1 -> c._2.map(_.teamId)).toMap
  val keyTeam: Map[String, Team] = teams.map(t => t.key -> t).toMap

  def firstGame: Option[LocalDate] = games.headOption.map(_.date)

  def lastGame: Option[LocalDate] = games.lastOption.map(_.date)

  def numResults: Int = games.size

  def firstResult: Option[LocalDate] = results.headOption.map(r => gameMap(r.gameId).date)

  def lastResult: Option[LocalDate] = results.lastOption.map(r => gameMap(r.gameId).date)

  def teamsMapped: List[Team] = teams.filter(t => teamConference.contains(t.id))
  def teamsMap: Map[Long,Team] = teams.map(t=> t.id->t).toMap

  def record(team: Team, predicate: Game => Boolean = _ => true, lastN: Int = 0): (Int, Int) = {
    val g1 = games.filter(g=>team.id == g.homeTeamId || team.id == g.awayTeamId).filter(predicate)
    (lastN match {
      case 0 => g1
      case n => g1.take(n)
    }).foldLeft(0, 0)((wl: (Int, Int), game: Game) => if (isWinner(team, game)) (wl._1+1,wl._2) else if (isLoser(team, game)) (wl._1, wl._2+1) else wl)
  }

  def overallRecord(team: Team): (Int, Int) = record(team)
  def conferenceRecord(team: Team): (Int, Int) = record(team,  g=> isConferenceGame( g))
  def conference(team:Team): Conference = conferenceIdMap(teamConference(team.id))
  def isWinner(t: Team, g: Game, r: Result): Boolean = {
    require(t.id == g.homeTeamId || t.id == g.awayTeamId)
    require(r.gameId == g.id)
    (t.id == g.homeTeamId && r.homeScore > r.awayScore) || (t.id == g.awayTeamId && r.awayScore > r.homeScore)
  }

  def isWinner(t: Team, g: Game): Boolean = {
    require(t.id == g.homeTeamId || t.id == g.awayTeamId)
    resultMap.get(g.id) match {
      case Some(r) => isWinner(t, g, r)
      case None => false
    }
  }

  def isLoser(t: Team, g: Game, r: Result): Boolean = {
    require(t.id == g.homeTeamId || t.id == g.awayTeamId)
    require(r.gameId == g.id)
    (t.id == g.homeTeamId && r.homeScore < r.awayScore) || (t.id == g.awayTeamId && r.awayScore < r.homeScore)
  }

  def isLoser(t: Team, g: Game): Boolean = {
    require(t.id == g.homeTeamId || t.id == g.awayTeamId)
    resultMap.get(g.id) match {
      case Some(r) => isLoser(t, g, r)
      case None => false
    }
  }

  def isConferenceGame( g:Game):Boolean = {
    val conferences = conferenceMap.filter(m=>m.teamId == g.homeTeamId || m.teamId==g.awayTeamId).map(_.conferenceId)
    conferences.size==2 && (conferences(0) == conferences(1))
  }

  def games(t:Team):List[(Game, Option[Result])] = {
    games.filter(g=>t.id == g.homeTeamId || t.id == g.awayTeamId).sortBy(_.date.toEpochDay).map(g=>g->resultMap.get(g.id))
  }

}
