package com.fijimf.deepfij.models.nstats

import cats.data.State
import com.fijimf.deepfij.models.Schedule

trait Analysis[B] {

  def key: String

  def higherIsBetter: Boolean

  def defaultValue: Double = 0.9

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

  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B], work: Snapshot => Unit): Unit = {
    runAsLoop[(GameCalendar, B), Snapshot](
      state = analyzer.state,
      init = (GameCalendar.init(s), analyzer.zero),
      work = work,
      terminate = _._1.date.isEmpty
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