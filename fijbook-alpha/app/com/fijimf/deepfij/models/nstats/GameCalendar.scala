package com.fijimf.deepfij.models.nstats

import java.time.LocalDate

import com.fijimf.deepfij.models.Schedule

final case class GameCalendar(schedule: Schedule, date: Option[LocalDate]) {
  def next(): GameCalendar = {
    date.map(d => {
      val tomorrow = d.plusDays(1)
      if (schedule.season.dates.contains(tomorrow)) {
        copy(date = Some(tomorrow))
      } else {
        copy(date = None)
      }
    }).getOrElse(this)
  }

  def scoreboard: Option[Scoreboard] = date.map(d => Scoreboard(d, schedule.completeGames.filter(_._1.date == d)))
}

object GameCalendar {
  def init(s: Schedule) = GameCalendar(s, s.season.dates.headOption)
}

