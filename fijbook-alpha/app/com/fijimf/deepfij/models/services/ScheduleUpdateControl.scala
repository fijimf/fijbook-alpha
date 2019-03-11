package com.fijimf.deepfij.models.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.Season

import scala.util.matching.Regex


sealed trait ScheduleUpdateControl {
  def season: Int

  def dates: Option[List[LocalDate]]

  def dateOK(d: LocalDate): Boolean = {
    !d.isBefore(Season.startDate(season)) && !d.isAfter(Season.endDate(season))
  }
}

case class WholeSeasonUpdate(season: Int) extends ScheduleUpdateControl {
  val dates = Option.empty[List[LocalDate]]
}

case class ToFromTodayUpdate(season: Int, daysBack: Int, daysAhead:Int) extends ScheduleUpdateControl {
  val today: LocalDate = LocalDate.now()
  val dates = Some((-1 * daysBack).to(daysAhead).map(today.plusDays(_)).filter(dateOK).toList)
}

case class SingleDateUpdate(season: Int, d: LocalDate) extends ScheduleUpdateControl {
  override def dates: Option[List[LocalDate]] = Some(List(d).filter(dateOK))
}

object UpdateControlString {
  val wholeSeason: Regex = """(\d{4})\.(?i)(all)""".r
  val seasonBeforeAndAfterNow: Regex = """(\d{4})\.(\d+)\.(d+)""".r
  val seasonDate: Regex = """(\d{4})\.(\d{8})""".r

  def apply(str: String): ScheduleUpdateControl = str match {
    case wholeSeason(s, _) => WholeSeasonUpdate(s.toInt)
    case seasonBeforeAndAfterNow(s, b, a) => ToFromTodayUpdate(s.toInt, b.toInt, a.toInt)
    case seasonDate(s, d) => SingleDateUpdate(s.toInt, LocalDate.parse(d, DateTimeFormatter.ofPattern("yyyyMMdd")))
  }
}

