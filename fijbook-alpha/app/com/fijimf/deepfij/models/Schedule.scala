package com.fijimf.deepfij.models

import java.time.{LocalDate, Month}

case class Schedule
(
  season: Season,
  teams: List[Team],
  conferences: List[Conference],
  conferenceMap: List[ConferenceMap],
  gameResults: List[(Game, Option[Result])],
  predictions: List[(Game, Option[GamePrediction])]
) {
  val conferenceKeyMap: Map[String, Conference] = conferences.map(t => t.key -> t).toMap
  val games: List[Game] = gameResults.map(_._1).sortBy(_.date.toEpochDay)
  val gameMap: Map[Long, Game] = games.map(g => g.id -> g).toMap
  val results: List[Result] = gameResults.filter(_._2.isDefined).sortBy(_._1.date.toEpochDay).map(_._2.get)
  val resultMap: Map[Long, Result] = results.map(r => r.gameId -> r).toMap
  val predictionMap: Map[Long, Map[String, GamePrediction]] =
    predictions.filter(_._2.isDefined)
      .groupBy(r => r._2.get.gameId)
      .mapValues(
        _.map(t => t._2.get.modelKey -> t._2.get).toMap
      )
  val conferenceIdMap: Map[Long, Conference] = conferences.map(r => r.id -> r).toMap
  val teamConference: Map[Long, Long] = conferenceMap.map(c => c.teamId -> c.conferenceId).toMap
  val conferenceTeams: Map[Long, List[Long]] = conferenceMap.groupBy(_.conferenceId).map(c => c._1 -> c._2.map(_.teamId)).toMap
  val keyTeam: Map[String, Team] = teams.map(t => t.key -> t).toMap

  def firstGame: Option[LocalDate] = games.headOption.map(_.date)

  def lastGame: Option[LocalDate] = games.lastOption.map(_.date)

  def numResults: Int = games.size

  def firstResult: Option[LocalDate] = resultDates.headOption

  def lastResult: Option[LocalDate] = resultDates.reverse.headOption

  def resultDates: List[LocalDate] = results.map(r => gameMap(r.gameId).date).sortBy(_.toEpochDay)

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

  def winner(g: Game): Option[Team] = {
    resultMap.get(g.id) match {
      case Some(r) => if (r.homeScore > r.awayScore) teamsMap.get(g.homeTeamId) else teamsMap.get(g.awayTeamId)
      case None => None
    }
  }

  def loser(g: Game): Option[Team] = {
    resultMap.get(g.id) match {
      case Some(r) => if (r.homeScore < r.awayScore) teamsMap.get(g.homeTeamId) else teamsMap.get(g.awayTeamId)
      case None => None
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
    val g1 = games.filter(g => team.id == g.homeTeamId || team.id == g.awayTeamId).filter(g => resultMap.contains(g.id) && predicate(g))
    val (w, l) = (lastN match {
      case 0 => g1
      case n => g1.takeRight(n)
    }).foldLeft(0, 0)((wl: (Int, Int), game: Game) => if (isWinner(team, game)) (wl._1 + 1, wl._2) else if (isLoser(team, game)) (wl._1, wl._2 + 1) else wl)
    WonLostRecord(w, l)
  }

  def interConfRecord(conf: Conference): List[(Conference, WonLostRecord)] = {
    val confGames: Map[Long, List[Game]] = nonConferenceSchedule(conf).groupBy(g => if (teamConference(g.awayTeamId) == conf.id) teamConference(g.homeTeamId) else teamConference(g.awayTeamId))
    confGames.mapValues(gl =>
      gl.foldLeft(0, 0)((wl: (Int, Int), game: Game) => {
        winner(game) match {
          case Some(w) => if (teamConference(w.id) == conf.id) (wl._1 + 1, wl._2) else (wl._1, wl._2 + 1)
          case _ => wl
        }
      })
    ).map((tuple: (Long, (Int, Int))) => (conferenceIdMap(tuple._1), WonLostRecord(tuple._2._1, tuple._2._2))).toList.sortBy(tup => (tup._2.lost - tup._2.won, -tup._2.won, tup._1.name))

  }

  def overallRecord(team: Team): WonLostRecord = record(team)

  def conferenceRecord(team: Team): WonLostRecord = record(team, g => isConferenceGame(g))

  def nonConferenceRecord(team: Team): WonLostRecord = record(team, g => !isConferenceGame(g))

  def homeRecord(team: Team): WonLostRecord = record(team, g => g.homeTeamId == team.id)

  def awayRecord(team: Team): WonLostRecord = record(team, g => g.awayTeamId == team.id)

  def neutralRecord(team: Team): WonLostRecord = record(team, g => g.isNeutralSite)

  def last5Record(team: Team): WonLostRecord = record(team, g => true, 5)

  def last10Record(team: Team): WonLostRecord = record(team, g => true, 10)

  def novRecord(team: Team): WonLostRecord = record(team, g => g.date.getMonth == Month.NOVEMBER)

  def decRecord(team: Team): WonLostRecord = record(team, g => g.date.getMonth == Month.DECEMBER)

  def janRecord(team: Team): WonLostRecord = record(team, g => g.date.getMonth == Month.JANUARY)

  def febRecord(team: Team): WonLostRecord = record(team, g => g.date.getMonth == Month.FEBRUARY)

  def marRecord(team: Team): WonLostRecord = record(team, g => g.date.getMonth == Month.MARCH)

  def conferenceStandings(conference: Conference): ConferenceStandings = {
    val list = conferenceTeams.get(conference.id) match {
      case Some(lst) => lst.map(tid => teamsMap(tid)).map(t => (conferenceRecord(t), overallRecord(t), t))
      case None => List.empty
    }
    ConferenceStandings(list.sortBy(tup => (tup._1.lost - tup._1.won, -tup._1.won, tup._2.lost - tup._2.won, -tup._2.won, tup._3.name)))
  }

  def conferenceSchedule(conference: Conference): List[Game] = {
    games.filter(g => teamConference.get(g.homeTeamId).contains(conference.id) && teamConference.get(g.awayTeamId).contains(conference.id))
  }

  def nonConferenceSchedule(conference: Conference): List[Game] = {
    games.filter(g => teamConference.get(g.homeTeamId) != teamConference.get(g.awayTeamId) && (teamConference.get(g.homeTeamId).contains(conference.id) || teamConference.get(g.awayTeamId).contains(conference.id)))
  }


  def teamHomeGamesByLocation = games.filter(_.tourneyKey.isEmpty).foldLeft(Map.empty[Long, Map[String, Int]])((locationData: Map[Long, Map[String, Int]], game: Game) => {
    game.location match {
      case Some(loc) => {
        locationData.get(game.homeTeamId) match {
          case Some(map) => {
            val count = map.getOrElse(loc, 0) + 1
            locationData + (game.homeTeamId -> (map + (loc -> count)))
          }
          case None => {
            locationData + (game.homeTeamId -> Map(loc -> 1))
          }
        }
      }
      case None => locationData
    }
  })

  def conferenceGamesByLocationDate = {
    val conferenceToGames: Map[Conference, List[Game]] = games.filter(isConferenceGame).groupBy(g => conference(teamsMap(g.homeTeamId)))
    conferenceToGames.map { case (conference: Conference, games: List[Game]) => {
      games.groupBy(g => (g.date, g.location))
    }
    }
  }
}
