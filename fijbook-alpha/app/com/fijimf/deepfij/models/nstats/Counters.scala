package com.fijimf.deepfij.models.nstats

import com.fijimf.deepfij.models.{Game, Result}

object Counters {

  trait base extends Analysis[Map[Long, Double]] {

    override def zero: Map[Long, Double] = Map.empty[Long, Double]

    override def update(os: Option[Scoreboard], b: Map[Long, Double]): Map[Long, Double] = os match {
      case Some(sb) =>
        sb.gs.foldLeft(b) {
          case (map, (game, result)) =>
            incrementKeys(game, result).foldLeft(map) {
              case (m1: Map[Long, Double], k: Long) => m1 + (k -> (m1.getOrElse(k, 0.0) + 1.0))
            }
        }

      case None => b
    }

    def incrementKeys(g: Game, r: Result): List[Long]

    override def extract(b: Map[Long, Double]): Map[Long, Double] = b
  }

  trait withReset extends Analysis[Map[Long, Double]] {

    override def zero: Map[Long, Double] = Map.empty[Long, Double]

    override def update(os: Option[Scoreboard], b: Map[Long, Double]): Map[Long, Double] = os match {
      case Some(sb) =>
        sb.gs.foldLeft(b) {
          case (map, (game, result)) =>
            val p = incrementKeys(game, result).foldLeft(map) {
              case (m1: Map[Long, Double], k: Long) => m1 + (k -> (m1.getOrElse(k, 0.0) + 1.0))
            }
            resetKeys(game, result).foldLeft(p) {
              case (p1: Map[Long, Double], k: Long) => p1 + (k -> 0.0)
            }
        }

      case None => b
    }

    def incrementKeys(g: Game, r: Result): List[Long]

    def resetKeys(g: Game, r: Result): List[Long]

    override def extract(b: Map[Long, Double]): Map[Long, Double] = b
  }


  object games extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = List(g.homeTeamId, g.awayTeamId)

    override def key: String = "n-games"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)
  }

  object wins extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore > r.awayScore) List(g.homeTeamId) else List(g.awayTeamId)

    override def key: String = "wins"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object losses extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore < r.awayScore) List(g.homeTeamId) else List(g.awayTeamId)

    override def key: String = "losses"

    override def higherIsBetter: Boolean = false

    override def bounds=(0,Double.PositiveInfinity)

  }

  object homeWins extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore > r.awayScore) List(g.homeTeamId) else List.empty

    override def key: String = "home-wins"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object homeLosses extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore < r.awayScore) List(g.homeTeamId) else List.empty

    override def key: String = "home-losses"

    override def higherIsBetter: Boolean = false

    override def bounds=(0,Double.PositiveInfinity)

  }

  object awayWins extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore < r.awayScore) List(g.awayTeamId) else List.empty

    override def key: String = "away-wins"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object awayLosses extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore > r.awayScore) List(g.awayTeamId) else List.empty

    override def key: String = "away-losses"

    override def higherIsBetter: Boolean = false

    override def bounds=(0,Double.PositiveInfinity)

  }

  object otGames extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.periods > 2) List(g.homeTeamId, g.awayTeamId) else List.empty

    override def key: String = "ot-games"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object otWins extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.periods > 2) wins.incrementKeys(g, r) else List.empty

    override def key: String = "ot-wins"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object otLosses extends base {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.periods > 2) losses.incrementKeys(g, r) else List.empty

    override def key: String = "ot-losses"

    override def higherIsBetter: Boolean = false

    override def bounds=(0,Double.PositiveInfinity)

  }

  object winStreak extends withReset {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore > r.awayScore) List(g.homeTeamId) else List(g.awayTeamId)

    override def resetKeys(g: Game, r: Result): List[Long] = if (r.homeScore > r.awayScore) List(g.awayTeamId) else List(g.homeTeamId)

    override def key: String = "win-streak"

    override def higherIsBetter: Boolean = true

    override def bounds=(0,Double.PositiveInfinity)

  }

  object lossStreak extends withReset {
    override def incrementKeys(g: Game, r: Result): List[Long] = if (r.homeScore < r.awayScore) List(g.homeTeamId) else List(g.awayTeamId)

    override def resetKeys(g: Game, r: Result): List[Long] = if (r.homeScore < r.awayScore) List(g.awayTeamId) else List(g.homeTeamId)

    override def key: String = "loss-streak"

    override def higherIsBetter: Boolean = false

    override def bounds=(0,Double.PositiveInfinity)

  }

}
