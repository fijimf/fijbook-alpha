package com.fijimf.deepfij.models.nstats

import cats.data.State
import com.fijimf.deepfij.models.Schedule

trait Analysis[B] {

  def key: String

  def fmtString="%7.3f"

  def higherIsBetter: Boolean

  def bounds:(Double, Double) = (Double.NegativeInfinity, Double.PositiveInfinity)

  def defaultValue: Double = 0.0

  def zero(s:Schedule): B

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
    "Win Streak" -> Counters.winStreak,
    "Loss Streak" -> Counters.lossStreak,
    "Win Pct" -> HigherOrderCounters.winPct,
    "Opp Wins" -> HigherOrderCounters.oppWins,
    "Opp Losses" -> HigherOrderCounters.oppLosses,
    "Opp WP" -> HigherOrderCounters.oppWinPct,
    "Opp Opp Wins" -> HigherOrderCounters.oppOppWins,
    "Opp Opp Losses" -> HigherOrderCounters.oppOppLosses,
    "Opp Opp WP" -> HigherOrderCounters.oppOppWinPct,
    "RPI" -> HigherOrderCounters.rpi,
    "OLS power" -> Regression.ols,
    "Avg Margin" -> Appenders.meanMargin,
    "Var Margin" -> Appenders.varianceMargin,
    "Max Margin" -> Appenders.maxMargin,
    "Min Margin" -> Appenders.minMargin,
    "Med Margin" -> Appenders.medianMargin,
    "Avg Comb Pts" -> Appenders.meanCombined,
    "Var Comb Pts" -> Appenders.varianceCombined,
    "Max Comb Pts" -> Appenders.maxCombined,
    "Min Comb Pts" -> Appenders.minCombined,
    "Med Comb Pts" -> Appenders.medianCombined,
    "Avg Pts For" -> Appenders.meanPointsFor,
    "Var Pts For" -> Appenders.variancePointsFor,
    "Max Pts For" -> Appenders.maxPointsFor,
    "Min Pts For" -> Appenders.minPointsFor,
    "Med Pts For" -> Appenders.medianPointsFor,
    "Avg Pts Against" -> Appenders.meanPointsAgainst,
    "Var Pts Against" -> Appenders.variancePointsAgainst,
    "Max Pts Against" -> Appenders.maxPointsAgainst,
    "Min Pts Against" -> Appenders.minPointsAgainst,
    "Med Pts Against" -> Appenders.medianPointsAgainst
  )


  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B], work: Snapshot => Unit): Unit = {
    runAsLoop[(GameCalendar, B), Snapshot](
      state = analyzer.state,
      init = (GameCalendar.init(s), analyzer.zero(s)),
      work = work,
      terminate = _._1.date.isEmpty
    )
  }

  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B], work: Snapshot => Unit, terminate:((GameCalendar, B))=>Boolean): Unit = {
    runAsLoop[(GameCalendar, B), Snapshot](
      state = analyzer.state,
      init = (GameCalendar.init(s), analyzer.zero(s)),
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