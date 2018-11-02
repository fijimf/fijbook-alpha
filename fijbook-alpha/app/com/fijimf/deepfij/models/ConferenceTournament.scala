package com.fijimf.deepfij.models

import cats.implicits._

object ConferenceTournament {
  def apply(s: Schedule): List[(Conference, Option[TournamentNode])] = {
    s.conferences.map(c => {
      val tournamentGames = s.completeGames.filter(_._1.tourneyKey.contains(c.key))
      c -> createNode(s, tournamentGames)
    })
  }

  private def createNode(s: Schedule, tournamentGames: List[(Game, Result)]): Option[TournamentNode] = {
    import s._
    val teams = tournamentGames.flatMap(t => List(t._1.homeTeamId, t._1.awayTeamId))

    teams.find(tid => !tournamentGames.exists(_.isLoser(tid))).map(champ => {
      val championshipGame = tournamentGames.filter(_.hasTeam(champ)).maxBy(_._1.date.toEpochDay)
      val remaining = tournamentGames.filter(_._1.id =!= championshipGame._1.id)

      val homeBracket = remaining.filter(_.hasTeam(championshipGame._1.awayTeamId))
      val awayBracket = remaining.filter(_.hasTeam(championshipGame._1.homeTeamId))

      TournamentNode(championshipGame._1, championshipGame._2, createNode(s, homeBracket), createNode(s, awayBracket))
    })
  }
}

case class TournamentNode(g: Game, r: Result, hg: Option[TournamentNode], ag: Option[TournamentNode]) {

  def maxDepth: Int = (hg, ag) match {
    case (None, None) => 1
    case (Some(tn), None) => 1 + tn.maxDepth
    case (None, Some(tn)) => 1 + tn.maxDepth
    case (Some(sn), Some(tn)) => 1 + Math.max(sn.maxDepth, tn.maxDepth)
  }

  def round(depth: Int): List[Game] = {
    if (depth === 0) {
      List(g)
    } else {
      hg.map(_.round(depth - 1)).getOrElse(List.empty[Game]) ++ ag.map(_.round(depth - 1)).getOrElse(List.empty[Game])
    }
  }
}


