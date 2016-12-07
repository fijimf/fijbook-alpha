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

  def teamsMap: Map[Long, Team] = teams.map(t => t.id -> t).toMap

  def conference(team: Team): Conference = conferenceIdMap(teamConference(team.id))

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

  def isConferenceGame(g: Game): Boolean = {
    val conferences = conferenceMap.filter(m => m.teamId == g.homeTeamId || m.teamId == g.awayTeamId).map(_.conferenceId)
    conferences.size == 2 && (conferences(0) == conferences(1))
  }

  def games(t: Team): List[(Game, Option[Result])] = {
    games.filter(g => t.id == g.homeTeamId || t.id == g.awayTeamId).sortBy(_.date.toEpochDay).map(g => g -> resultMap.get(g.id))
  }


  def gameLine(t: Team, g: Game, or: Option[Result]): GameLine = {
    val date = g.date
    val (vsAt, opp) = if (t.id == g.homeTeamId) ("vs.", teamsMap(g.awayTeamId)) else ("@", teamsMap(g.homeTeamId))
    val (wl, literalScore) = or match {
      case Some(r) => {
        (if (isWinner(t, g, r)) "W" else if (isLoser(t, g, r)) "L" else "", s"${r.homeScore} - ${r.awayScore}")
      }
      case None => ("", "")
    }
    GameLine(date, vsAt, opp, wl, literalScore)
  }

  def record(team: Team, predicate: Game => Boolean = _ => true, lastN: Int = 0): WonLostRecord = {
    val g1 = games.filter(g => team.id == g.homeTeamId || team.id == g.awayTeamId).filter(predicate)
   val(w,l) =  (lastN match {
      case 0 => g1
      case n => g1.take(n)
    }).foldLeft(0, 0)((wl: (Int, Int), game: Game) => if (isWinner(team, game)) (wl._1 + 1, wl._2) else if (isLoser(team, game)) (wl._1, wl._2 + 1) else wl)
    WonLostRecord(w,l)
  }

  def overallRecord(team: Team): WonLostRecord = record(team)

  def conferenceRecord(team: Team): WonLostRecord = record(team, g => isConferenceGame(g))
  def nonConferenceRecord(team: Team): WonLostRecord = record(team, g => !isConferenceGame(g))

  def homeRecord(team: Team):WonLostRecord = record(team, g => g.homeTeamId == team.id)

  def awayRecord(team: Team):WonLostRecord = record(team, g => g.awayTeamId == team.id)

  def neutralRecord(team: Team):WonLostRecord = record(team, g => g.isNeutralSite)

  case class WonLostRecord(won: Int = 0, lost: Int = 0) extends Ordering[WonLostRecord] {
    require(won>=0 && lost>=0)
    override def compare(x: WonLostRecord, y: WonLostRecord): Int = {
      (x.won - x.lost) - (y.won - y.lost) match {
        case 0 => x.won - y.won
        case i => i
      }
    }
    def pct:Option[Double] =
      won + lost match {
        case 0=>None
        case x=>Some(won.toDouble/x.toDouble)
      }
  }

  case class ConferenceStandings(records:List[(WonLostRecord, WonLostRecord, Team)])

  def conferenceStandings(conference: Conference): ConferenceStandings = {
    val list = conferenceTeams(conference.id).map(tid => teamsMap(tid)).map(t => ( conferenceRecord(t), overallRecord(t),t))
    ConferenceStandings(list.sortBy(tup=>(tup._1.lost-tup._1.won, -tup._1.won, tup._2.lost-tup._2.won, -tup._2.won,tup._3.name)))
  }


}
