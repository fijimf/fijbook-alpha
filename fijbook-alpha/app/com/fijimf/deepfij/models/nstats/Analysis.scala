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
    "Games" -> Counter.games,
    "Wins" -> Counter.wins,
    "Losses" -> Counter.losses,
    "Win Streak" -> Counter.winStreak,
    "Loss Streak" -> Counter.lossStreak,
    "Win Pct" -> HigherOrderCounters.winPct,
    "Opp Wins" -> HigherOrderCounters.oppWins,
    "Opp Losses" -> HigherOrderCounters.oppLosses,
    "Opp WP" -> HigherOrderCounters.oppWinPct,
    "Opp Opp Wins" -> HigherOrderCounters.oppOppWins,
    "Opp Opp Losses" -> HigherOrderCounters.oppOppLosses,
    "Opp Opp WP" -> HigherOrderCounters.oppOppWinPct,
    "RPI" -> HigherOrderCounters.rpi,
    "OLS power" -> Regression.ols,
    "Avg Margin" -> Appender.meanMargin,
    "Var Margin" -> Appender.varianceMargin,
    "Max Margin" -> Appender.maxMargin,
    "Min Margin" -> Appender.minMargin,
    "Med Margin" -> Appender.medianMargin,
    "Avg Comb Pts" -> Appender.meanCombined,
    "Var Comb Pts" -> Appender.varianceCombined,
    "Max Comb Pts" -> Appender.maxCombined,
    "Min Comb Pts" -> Appender.minCombined,
    "Med Comb Pts" -> Appender.medianCombined,
    "Avg Pts For" -> Appender.meanPointsFor,
    "Var Pts For" -> Appender.variancePointsFor,
    "Max Pts For" -> Appender.maxPointsFor,
    "Min Pts For" -> Appender.minPointsFor,
    "Med Pts For" -> Appender.medianPointsFor,
    "Avg Pts Against" -> Appender.meanPointsAgainst,
    "Var Pts Against" -> Appender.variancePointsAgainst,
    "Max Pts Against" -> Appender.maxPointsAgainst,
    "Min Pts Against" -> Appender.minPointsAgainst,
    "Med Pts Against" -> Appender.medianPointsAgainst
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