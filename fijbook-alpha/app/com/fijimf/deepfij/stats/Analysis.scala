package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Schedule, Team}
import org.apache.commons.math3.stat.StatUtils


case class Stat[S](name: String, key: String, defaultValue: Double, higherIsBetter: Boolean, f: S => Double)

trait Analyzer[S] {
  val data: Map[LocalDate, Map[Team, S]]
  val stats: List[Stat[S]]

  def value(k: String, t: Team, d: LocalDate): Option[Double] = {
    stats.find(_.key == k) match {
      case Some(s) => {
        data.get(d) match {
          case Some(m) =>
            m.get(t) match {
              case Some(u: S) => Some(s.f(u))
              case None => Some(s.defaultValue)
            }
          case None => None
        }
      }
      case None => None
    }
  }
}

case class WonLostAccumulator(wins: Int = 0, losses: Int = 0, winStreak: Int = 0, lossStreak: Int = 0) {
  def addWin() = WonLostAccumulator(wins + 1, losses, winStreak + 1, 0)

  def addLoss() = WonLostAccumulator(wins, losses + 1, 0, lossStreak + 1)

  val winPct: Double = if (wins + losses == 0) 0.0 else wins.toDouble / (wins + losses).toDouble
}

class WonLost(s: Schedule) extends Analyzer[ScoringAccumulator] {

  val data: Map[LocalDate, Map[Team, WonLostAccumulator]] = {
    val zero = (Map.empty[Team, WonLostAccumulator], Map.empty[LocalDate, Map[Team, WonLostAccumulator]])
    s.games
      .sortBy(_.date.toEpochDay)
      .foldLeft(zero)((tuple: (Map[Team, WonLostAccumulator], Map[LocalDate, Map[Team, WonLostAccumulator]]), game: Game) => {
        val (r0, byDate) = tuple
        val r1 = s.winner(game) match {
          case Some(t) => {
            r0 + (t -> r0.getOrElse(t, WonLostAccumulator()).addWin())
          }
          case None => r0
        }
        val r2 = s.loser(game) match {
          case Some(t) => {
            r1 + (t -> r1.getOrElse(t, WonLostAccumulator()).addLoss())
          }
          case None => r1
        }
        (r2, byDate + (game.date -> r2))
      })._2
  }

  val stats: List[Stat[WonLostAccumulator]] = List(
    Stat[WonLostAccumulator]("Wins", "wins", 0, higherIsBetter = true, _.wins),
    Stat[WonLostAccumulator]("Losses", "losses", 0, higherIsBetter = true, _.losses),
    Stat[WonLostAccumulator]("Winning Streak", "wstreak", 0, higherIsBetter = true, _.lossStreak),
    Stat[WonLostAccumulator]("Losing Streak", "lstreak", 0, higherIsBetter = false, _.winStreak),
    Stat[WonLostAccumulator]("Winning Pct.", "wp", 0, higherIsBetter = true, _.winPct)
  )


}

case class RpiAccumulator(wins: Int = 0, losses: Int = 0, oppWins: Int = 0, oppLosses: Int = 0, oppOppWins: Int = 0, oppOppLosses: Int = 0) {
  def addWin() = copy(wins = wins + 1)

  def addLoss() = copy(losses = losses + 1)

  def addOppWins(n: Int) = copy(oppWins = oppWins + n)

  def addOppLosses(n: Int) = copy(oppLosses = oppLosses + n)

  def addOppOppWins(n: Int) = copy(oppOppWins = oppOppWins + n)

  def addOppOppLosses(n: Int) = copy(oppOppLosses = oppOppLosses + n)

  val winPct: Double = if (wins + losses == 0) 0.0 else wins.toDouble / (wins + losses).toDouble
  val oppWinPct: Double = if (oppWins + oppOppLosses == 0) 0.0 else oppWins.toDouble / (oppWins + oppOppLosses).toDouble
  val oppOppWinPct: Double = if (oppOppWins + oppOppLosses == 0) 0.0 else oppOppWins.toDouble / (oppOppWins + oppOppLosses).toDouble

  def rpi(x: Int = 1, y: Int = 1, z: Int = 1) = (x * winPct + y * oppWinPct + z * oppOppWinPct) / (x + y + z)

  def rpig(x: Int = 1, y: Int = 1, z: Int = 1) = math.pow(math.pow(winPct, x) * math.pow(oppWinPct, y) * math.pow(oppOppWinPct, z), -(x + y + z))
}

class Rpi(s: Schedule) extends Analyzer[RpiAccumulator] {

  val data: Map[LocalDate, Map[Team, RpiAccumulator]] = {
    val zero = (Map.empty[Team, RpiAccumulator], Map.empty[LocalDate, Map[Team, RpiAccumulator]])
    s.games
      .sortBy(_.date.toEpochDay)
      .foldLeft(zero)((tuple: (Map[Team, RpiAccumulator], Map[LocalDate, Map[Team, RpiAccumulator]]), game: Game) => {
        val (running, byDate) = tuple
        val newRunning = (s.winner(game), s.loser(game)) match {
          case (Some(w), Some(l)) => {
            val w0 = running.getOrElse(w, RpiAccumulator())
            val l0 = running.getOrElse(l, RpiAccumulator())
            val w1 = w0.addOppOppWins(l0.oppWins).addOppOppLosses(l0.oppLosses)
            val l1 = l0.addOppOppWins(w0.oppWins).addOppOppLosses(w0.oppLosses)
            val w2 = w1.addOppWins(l1.wins).addOppLosses(l1.losses)
            val l2 = l1.addOppWins(w1.wins).addOppLosses(w1.losses)
            val w3 = w2.addWin()
            val l3 = l2.addLoss()
            running + (w -> w3, l -> l3)
          }
          case _ => running
        }
        (newRunning, byDate + (game.date -> newRunning))
      })._2
  }

  val stats: List[Stat[RpiAccumulator]] = List(
    Stat[RpiAccumulator]("Wins", "wins", 0, higherIsBetter = true, _.wins),
    Stat[RpiAccumulator]("Losses", "losses", 0, higherIsBetter = false, _.losses),
    Stat[RpiAccumulator]("Opp Wins", "oppwins", 0, higherIsBetter = true, _.oppOppWins),
    Stat[RpiAccumulator]("Opp Losses", "opplosses", 0, higherIsBetter = false, _.oppLosses),
    Stat[RpiAccumulator]("Opp Opp Wins", "oppoppwins", 0, higherIsBetter = true, _.oppOppWins),
    Stat[RpiAccumulator]("Opp Opp Losses", "oppopplosses", 0, higherIsBetter = false, _.oppOppLosses),
    Stat[RpiAccumulator]("Winning Pct.", "wp", 0, higherIsBetter = true, _.winPct),
    Stat[RpiAccumulator]("Opp Winning Pct.", "oppwp", 0, higherIsBetter = true, _.oppWinPct),
    Stat[RpiAccumulator]("Opp Opp Winning Pct.", "oppoppwp", 0, higherIsBetter = true, _.oppOppWinPct),
    Stat[RpiAccumulator]("RPI [1-2-1]", "rpi121", 0, higherIsBetter = true, _.rpi(1, 2, 1)),
    Stat[RpiAccumulator]("RPI [3-2-1]", "rpi321", 0, higherIsBetter = true, _.rpi(3, 2, 1)),
    Stat[RpiAccumulator]("RPI Geom [1-2-1]", "rpig121", 0, higherIsBetter = true, _.rpig(1, 2, 1)),
    Stat[RpiAccumulator]("RPI Geom [3-2-1]", "rpig321", 0, higherIsBetter = true, _.rpig(3, 2, 1))
  )


}

case class ScoringAccumulator(pointsFor: List[Int] = List.empty[Int], pointsAgainst: List[Int] = List.empty[Int], margin: List[Int] = List.empty[Int], overUnder: List[Int] = List.empty) {
  def addGame(score: Int, oppScore: Int) = ScoringAccumulator(score :: pointsFor, oppScore :: pointsAgainst, (score - oppScore) :: margin, (score + oppScore) :: overUnder)
}

case class Scoring(s: Schedule) extends Analyzer[ScoringAccumulator] {

  val data: Map[LocalDate, Map[Team, ScoringAccumulator]] = {
    val zero = (Map.empty[Team, ScoringAccumulator], Map.empty[LocalDate, Map[Team, ScoringAccumulator]])
    s.games
      .sortBy(_.date.toEpochDay)
      .foldLeft(zero)((tuple: (Map[Team, ScoringAccumulator], Map[LocalDate, Map[Team, ScoringAccumulator]]), game: Game) => {
        val (r0, byDate) = tuple
        s.resultMap.get(game.id) match {
          case Some(result) => {
            val r1: Map[Team, ScoringAccumulator] = s.teamsMap.get(game.homeTeamId) match {
              case Some(t) => {
                r0 + (t -> r0.getOrElse(t, ScoringAccumulator()).addGame(result.homeScore, result.awayScore))
              }
              case None => r0
            }
            val r2 = s.teamsMap.get(game.awayTeamId) match {
              case Some(t) => {
                r1 + (t -> r1.getOrElse(t, ScoringAccumulator()).addGame(result.awayScore, result.homeScore))
              }
              case None => r1
            }
            (r2, byDate + (game.date -> r2))

          }
          case None => (r0, byDate)
        }
      })._2
  }


  val stats = List(
    Stat[ScoringAccumulator]("Mean Points For", "meanpf", 0, higherIsBetter = true, a => StatUtils.mean(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points For Variance", "varpf", 0, higherIsBetter = true, a => StatUtils.variance(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points For", "minpf", 0, higherIsBetter = true, a => StatUtils.min(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points For", "maxpf", 0, higherIsBetter = true, a => StatUtils.max(a.pointsFor.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Points Against", "meanpa", 0, higherIsBetter = true, a => StatUtils.mean(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Points Against Variance", "varpa", 0, higherIsBetter = true, a => StatUtils.variance(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Points Against", "minpa", 0, higherIsBetter = true, a => StatUtils.min(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Points Against", "maxpa", 0, higherIsBetter = true, a => StatUtils.max(a.pointsAgainst.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Scoring Margin", "meanmrgp", 0, higherIsBetter = true, a => StatUtils.mean(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Scoring Margin Variance", "varmrg", 0, higherIsBetter = true, a => StatUtils.variance(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Scoring Margin", "minmrg", 0, higherIsBetter = true, a => StatUtils.min(a.margin.map(_.toDouble).map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Scoring Margin", "maxmrg", 0, higherIsBetter = true, a => StatUtils.max(a.margin.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Mean Combined Score", "meanou", 0, higherIsBetter = true, a => StatUtils.mean(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Combined Score Variance", "varou", 0, higherIsBetter = true, a => StatUtils.variance(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Min Combined Score", "minou", 0, higherIsBetter = true, a => StatUtils.min(a.overUnder.map(_.toDouble).toArray)),
    Stat[ScoringAccumulator]("Max Combined Score", "maxou", 0, higherIsBetter = true, a => StatUtils.max(a.overUnder.map(_.toDouble).toArray))
  )
}



