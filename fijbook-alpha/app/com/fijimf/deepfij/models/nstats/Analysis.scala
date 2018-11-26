package com.fijimf.deepfij.models.nstats

import cats.data.State
import com.fijimf.deepfij.models.Schedule

trait Analysis[B] {

  def key: String

  def fmtString="%7.3f"

  def higherIsBetter: Boolean

  def bounds:(Double, Double) = (Double.NegativeInfinity, Double.PositiveInfinity)

  def defaultValue: Double = 0.0

  def zero: B

  def update(os: Option[Scoreboard], b: B): B

  def extract(b: B): Map[Long, Double]

  val state: State[(GameCalendar, B), Snapshot] = State[(GameCalendar, B), Snapshot] {
    case (cal, b) =>
      val b1 = update(cal.scoreboard, b)
      ((cal.next(), b1), Snapshot(cal.date.get, extract(b1)))
  }
}


object Analysis {

  val models: List[(String, Analysis[_])] = List(
    "Games" -> Counters.games,
    "Wins" -> Counters.wins,
    "Losses" -> Counters.losses,
    "Home Ws" -> Counters.homeWins,
    "Home Ls" -> Counters.homeLosses,
    "Away Ws" -> Counters.awayWins,
    "Away Ls" -> Counters.awayLosses,
    "OT Games" -> Counters.otGames,
    "OT Ws" -> Counters.otWins,
    "OT Ls" -> Counters.otLosses,
    "W Strk" -> Counters.winStreak,
    "L Strk" -> Counters.lossStreak,
    "Avg Mrg" -> Appenders.meanMargin,
    "Var Mrg" -> Appenders.varianceMargin,
    "Max Mrg" -> Appenders.maxMargin,
    "Min Mrg" -> Appenders.minMargin,
    "Med Mrg" -> Appenders.medianMargin,
    "Avg Cmb" -> Appenders.meanCombined,
    "Var Cmb" -> Appenders.varianceCombined,
    "Max Cmb" -> Appenders.maxCombined,
    "Min Cmb" -> Appenders.minCombined,
    "Med Cmb" -> Appenders.medianCombined,
    "Avg PF" -> Appenders.meanPointsFor,
    "Var PF" -> Appenders.variancePointsFor,
    "Max PF" -> Appenders.maxPointsFor,
    "Min PF" -> Appenders.minPointsFor,
    "Med PF" -> Appenders.medianPointsFor,
    "Avg PA" -> Appenders.meanPointsAgainst,
    "Var PA" -> Appenders.variancePointsAgainst,
    "Max PA" -> Appenders.maxPointsAgainst,
    "Min PA" -> Appenders.minPointsAgainst,
    "Med PA" -> Appenders.medianPointsAgainst,
    "Wins" -> HigherOrderCounters.wins,
    "Losses" -> HigherOrderCounters.losses,
    "Win Pct" -> HigherOrderCounters.winPct,
    "Opp Wins" -> HigherOrderCounters.oppWins,
    "Opp Losses" -> HigherOrderCounters.oppLosses,
    "Opp WP" -> HigherOrderCounters.oppWinPct,
    "Opp Opp Wins" -> HigherOrderCounters.oppOppWins,
    "Opp Opp Losses" -> HigherOrderCounters.oppOppLosses,
    "Opp Opp WP" -> HigherOrderCounters.oppOppWinPct,
    "RPI" -> HigherOrderCounters.rpi,
    "OLS power" -> Regression.ols
  )


  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B], work: Snapshot => Unit): Unit = {
    runAsLoop[(GameCalendar, B), Snapshot](
      state = analyzer.state,
      init = (GameCalendar.init(s), analyzer.zero),
      work = work,
      terminate = _._1.date.isEmpty
    )
  }

  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B], work: Snapshot => Unit, terminate:((GameCalendar, B))=>Boolean): Unit = {
    runAsLoop[(GameCalendar, B), Snapshot](
      state = analyzer.state,
      init = (GameCalendar.init(s), analyzer.zero),
      work = work,
      terminate = terminate
    )
  }

  def runAsLoop[S, A](state: State[S, A], init: S, work: A => Unit, terminate: S => Boolean): Unit = {
    loop(init)

    def loop(s: S): Unit = {
      val (h, b) = state.run(s).value
      work(b)
      if (!terminate(h)) {
        loop(h)
      }
    }
  }
}