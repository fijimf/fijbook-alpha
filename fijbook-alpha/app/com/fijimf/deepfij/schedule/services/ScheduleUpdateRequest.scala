package com.fijimf.deepfij.schedule.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.Season

import scala.util.matching.Regex


sealed trait ScheduleUpdateRequest {
  def season: Int

  def dates: Option[List[LocalDate]]

  def dateOK(d: LocalDate): Boolean = {
    !d.isBefore(Season.startDate(season)) && !d.isAfter(Season.endDate(season))
  }

  def dateToYear(d: LocalDate): Int = if (d.getMonth.getValue < 6) d.getYear else d.getYear - 1
}

final case class WholeSeasonUpdate(season: Int) extends ScheduleUpdateRequest {
  val dates = Option.empty[List[LocalDate]]
}

final case class ToFromTodayUpdate(daysBack: Int, daysAhead: Int) extends ScheduleUpdateRequest {
  val today: LocalDate = LocalDate.now()
  val season: Int = dateToYear(today)
  val dates = Some((-1 * daysBack).to(daysAhead).map(today.plusDays(_)).filter(dateOK).toList)
}

final case class SingleDateUpdate(d: LocalDate) extends ScheduleUpdateRequest {
  val season: Int = dateToYear(d)
  val dates: Option[List[LocalDate]] = Some(List(d).filter(dateOK))
}

object ScheduleUpdateControl {
  val wholeSeason: Regex = """(\d{4})\.(?i)(all)""".r
  val seasonBeforeAndAfterNow: Regex = """(\d+)\.(d+)""".r
  val seasonDate: Regex = """(\d{8})""".r

  def apply(str: String): ScheduleUpdateRequest = str match {
    case wholeSeason(s, _) => WholeSeasonUpdate(s.toInt)
    case seasonBeforeAndAfterNow( b, a) => ToFromTodayUpdate( b.toInt, a.toInt)
    case seasonDate( d) => SingleDateUpdate( LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd")))
  }
}

