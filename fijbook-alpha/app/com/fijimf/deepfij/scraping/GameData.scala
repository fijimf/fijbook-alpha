package com.fijimf.deepfij.scraping

package modules.scraping.model

import java.time.LocalDateTime

final case class GameData
(
  date: LocalDateTime,
  homeTeamKey: String,
  awayTeamKey: String,
  result: Option[ResultData],
  location: Option[String],
  tourneyInfo: Option[TourneyInfo],
  confInfo: String,
  sourceKey:String
)

final case class ResultData(homeScore: Int, awayScore: Int, periods: Int) {
  def margin = Math.abs(homeScore - awayScore)
}

final case class TourneyInfo(region: String, homeTeamSeed: Int, awayTeamSeed: Int)