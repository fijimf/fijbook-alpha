package com.fijimf.deepfij.models.nstats

import com.fijimf.deepfij.models.nstats.Counter.ResultTest
import com.fijimf.deepfij.models.{Game, Result, Schedule}

object Counter {
  type ResultTest = (Game, Result) => Boolean
  val games = Counter("n-games")
  val wins = Counter("wins",
    incrementHome = (_, r) => r.isHomeWinner,
    incrementAway = (_, r) => r.isAwayWinner
  )
  val losses = Counter("losses",
    higherIsBetter = false,
    incrementHome = (_, r) => r.isHomeLoser,
    incrementAway = (_, r) => r.isAwayLoser
  )
  val homeWins = Counter("home-wins",
    incrementHome = (_, r) => r.isHomeWinner,
    incrementAway = (_, _) => false
  )
  val homeLosses = Counter("home-losses",
    higherIsBetter = false,
    incrementHome = (_, r) => r.isHomeLoser,
    incrementAway = (_, _) => false
  )
  val awayWins = Counter("away-wins",
    incrementHome = (_, _) => false,
    incrementAway = (_, r) => r.isAwayWinner
  )
  val awayLosses = Counter("away-losses",
    higherIsBetter = false,
    incrementHome = (_, _) => false,
    incrementAway = (_, r) => r.isAwayLoser
  )
  val otGames = Counter("ot-games",
    incrementHome = (_, r) => r.periods > 2,
    incrementAway = (_, r) => r.periods > 2
  )
  val otWins = Counter("ot-wins",
    incrementHome = (_, r) => r.periods > 2 && r.isHomeWinner,
    incrementAway = (_, r) => r.periods > 2 && r.isAwayWinner
  )
  val otLosses = Counter("ot-losses",
    higherIsBetter = false,
    incrementHome = (_, r) => r.periods > 2 && r.isHomeLoser,
    incrementAway = (_, r) => r.periods > 2 && r.isAwayLoser
  )

  val winStreak = Counter("win-streak",
    incrementHome = (_, r) => r.isHomeWinner,
    incrementAway = (_, r) => r.isAwayWinner,
    resetHome = (_, r) => r.isHomeLoser,
    resetAway = (_, r) => r.isAwayLoser
  )
  val lossStreak = Counter("loss-streak",
    higherIsBetter = false,
    incrementHome = (_, r) => r.isHomeLoser,
    incrementAway = (_, r) => r.isAwayLoser,
    resetHome = (_, r) => r.isHomeWinner,
    resetAway = (_, r) => r.isAwayWinner
  )
}

case class Counter
(
  key: String,
  higherIsBetter: Boolean = true,
  incrementHome: ResultTest = (_, _) => true,
  incrementAway: ResultTest = (_, _) => true,
  resetHome: ResultTest = (_, _) => false,
  resetAway: ResultTest = (_, _) => false
) extends Analysis[Map[Long, Double]] {

  override def zero(s: Schedule): Map[Long, Double] = s.teams.map(_.id -> 0.0).toMap

  override def update(os: Option[Scoreboard], b: Map[Long, Double]): Map[Long, Double] = os match {
    case Some(sb) =>
      sb.gs.foldLeft(b) {
        case (map, (game, result)) =>
          val p = keysToIncrement(game, result).foldLeft(map) {
            case (m1: Map[Long, Double], k: Long) => m1 + (k -> (m1.getOrElse(k, 0.0) + 1.0))
          }
          keysToReset(game, result).foldLeft(p) {
            case (p1: Map[Long, Double], k: Long) => p1 + (k -> 0.0)
          }
      }
    case None => b
  }

  override def extract(b: Map[Long, Double]): Map[Long, Double] = b

  override val bounds: (Double, Double) = (0, Double.PositiveInfinity)

  private def keysToIncrement(g: Game, r: Result): List[Long] =
    List(
      if (incrementHome(g, r)) Some(g.homeTeamId) else None,
      if (incrementAway(g, r)) Some(g.awayTeamId) else None
    ).flatten

  private def keysToReset(g: Game, r: Result): List[Long] =
    List(
      if (resetHome(g, r)) Some(g.homeTeamId) else None,
      if (resetAway(g, r)) Some(g.awayTeamId) else None
    ).flatten

}
