package com.fijimf.deepfij.models

import java.time.LocalDateTime


case class GameInfo
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
)

