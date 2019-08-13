package com.fijimf.deepfij.models

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.scraping.model.ScoreboardByDateReq

import scala.util.control.Exception._


final case class GameInfo
(
  seasonId: Long,
  id: Long,
  resultId: Option[Long],
  datetime: LocalDateTime,
  homeTeamName: String,
  homeTeamScore: Option[Int],
  awayTeamName: String,
  awayTeamScore: Option[Int],
  periods: Option[Int],
  location: String,
  isNeutralSite: Boolean,
  tourney: String,
  homeSeed: Option[Int],
  awaySeed: Option[Int],
  source: String,
  updatedAt: LocalDateTime
) {
  def matches(str: String): Boolean = {
    val key = str.toLowerCase()
    datetime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).contains(key) ||
      homeTeamName.toLowerCase().contains(key) ||
      awayTeamName.toLowerCase().contains(key) ||
      location.toLowerCase().contains(key) ||
      tourney.toLowerCase().contains(key) ||
      source.toLowerCase().contains(key)
  }

  def sourceDate : Option[LocalDate] = catching(classOf[DateTimeParseException]).opt(LocalDate.parse(source))
  def sourceUrl : Option[String] = sourceDate.map(ScoreboardByDateReq(_).url)
}

