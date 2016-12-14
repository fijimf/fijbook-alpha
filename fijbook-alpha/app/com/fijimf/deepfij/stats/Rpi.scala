package com.fijimf.deepfij.stats

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Schedule, Team}

class Rpi(s: Schedule) extends Analyzer[RpiAccumulator] {

  val data: Map[LocalDate, Map[Team, RpiAccumulator]] = {
    val zero = (Map.empty[Team, RpiAccumulator], Map.empty[LocalDate, Map[Team, RpiAccumulator]])
    s.games
      .sortBy(_.date.toEpochDay)
      .foldLeft(zero)((tuple: (Map[Team, RpiAccumulator], Map[LocalDate, Map[Team, RpiAccumulator]]), game: Game) => {
        val (running, byDate) = tuple
        val newRunning = (s.winner(game), s.loser(game)) match {
          case (Some(w), Some(l)) =>
            val w0 = running.getOrElse(w, RpiAccumulator())
            val l0 = running.getOrElse(l, RpiAccumulator())
            val w1 = w0.addOppOppWins(l0.oppWins).addOppOppLosses(l0.oppLosses)
            val l1 = l0.addOppOppWins(w0.oppWins).addOppOppLosses(w0.oppLosses)
            val w2 = w1.addOppWins(l1.wins).addOppLosses(l1.losses)
            val l2 = l1.addOppWins(w1.wins).addOppLosses(w1.losses)
            val w3 = w2.addWin()
            val l3 = l2.addLoss()
            running + (w -> w3, l -> l3)
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
    Stat[RpiAccumulator]("RPI [4-2-1]", "rpi321", 0, higherIsBetter = true, _.rpi(4, 2, 1)),
    Stat[RpiAccumulator]("RPI Geom [1-2-1]", "rpig121", 0, higherIsBetter = true, _.rpig(1, 2, 1)),
    Stat[RpiAccumulator]("RPI Geom [4-2-1]", "rpig321", 0, higherIsBetter = true, _.rpig(4, 2, 1))
  )
  override val name: String = "RPI"
  override val desc: String = "Simple implementation of the NCAA's ratings percentage index, with possibly minor implementation details. 1-2-1 weight OWP twice as much as WP and Opp Opp WP, 4-2-1 weights WP twice as much as Opp WP and Opp WP twice as much as Opp Opp WP.  Also offers geometric as well as arithmetic average"
  override val key: String = "rpi"
}
