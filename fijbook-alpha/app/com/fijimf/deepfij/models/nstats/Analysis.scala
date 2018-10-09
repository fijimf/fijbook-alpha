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

  val state: State[(GameCalendar, B), Map[Long, Double]] = State[(GameCalendar, B), Map[Long, Double]] {
    case (cal, b) =>
      val b1 = update(cal.scoreboard, b)
      ((cal.next(), b1), extract(b1))
  }
}


object Analysis {

  def analyzeSchedule[B](s: Schedule, analyzer: Analysis[B]): Unit = {
    runAsLoop[(GameCalendar, B), Map[Long, Double]](analyzer.state, (GameCalendar.init(s), analyzer.zero), println(_), _._1.date.isEmpty)
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