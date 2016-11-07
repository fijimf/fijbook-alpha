package com.fijimf.deepfij.scraping

package modules.scraping.model

import java.time.LocalDateTime

case class GameData(date: LocalDateTime, homeTeamKey: String, awayTeamKey: String, result: Option[Result], location: Option[String], tourneyInfo: Option[TourneyInfo], confInfo: String)

case class Result(homeScore: Int, awayScore: Int, periods: Int) {
  def margin = Math.abs(homeScore - awayScore)
}

case class TourneyInfo(region: String, homeTeamSeed: Int, awayTeamSeed: Int)