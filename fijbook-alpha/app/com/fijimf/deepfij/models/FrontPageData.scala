package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.schedule.model.Schedule
import controllers.Utils._
final case class FrontPageData
(
  today: LocalDate,
  oSchedule:Option[Schedule],
  todaysGames: List[(Game, Option[Result])],
  previousGames: List[(Game, Option[Result])],
  qotw: Quote,
  qotwVotes:Int,
  qotwLastVote:LocalDateTime,
  newsItems: List[(RssItem, RssFeed)]
) {

  val yesterday: LocalDate = today.minusDays(1)
  def todayStr: String = if (today == LocalDate.now()){
    "Today's Games"
  } else {
    today.fmt("MMMM d, yyyy")
  }
  def prevStr: String = if (today == LocalDate.now()){
    "Yesterday's Games"
  } else {
    yesterday.fmt("MMMM d, yyyy")
  }

  val prevKey: String = today.minusDays(1).fmt("yyyyMMdd")
  val nextKey: String = today.plusDays(1).fmt("yyyyMMdd")
}
