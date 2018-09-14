package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

case class FrontPageData
(
  today: LocalDate,
  oSchedule:Option[Schedule],
  todaysGames: List[(Game, Option[Result])],
  previousResults: List[(Game, Option[Result])],
  qotw: Quote,
  qotwVotes:Int,
  qotwLastVote:LocalDateTime,
  newsItems: List[(RssItem, RssFeed)]
) {

}
