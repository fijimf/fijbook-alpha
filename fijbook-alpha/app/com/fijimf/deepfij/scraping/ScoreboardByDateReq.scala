package com.fijimf.deepfij.scraping

import java.time.LocalDate

import com.fijimf.deepfij.scraping.modules.scraping.model.GameData
import play.api.Logger
import play.api.libs.json.JsValue

import scala.util.{Failure, Success}

case class ScoreboardByDateReq(date: LocalDate, path: String = "http://data.ncaa.com/jsonp/scoreboard/basketball-men/d1/%04d/%02d/%02d/scoreboard.html") extends JsonScrapeRequest[List[GameData]] {
  val logger = Logger(this.getClass)
  override def url = path.format( date.getYear, date.getMonthValue ,date.getDayOfMonth )
  override def preProcessBody(s: String) = NcaaComGameScraper.stripCallbackWrapper(s)
  override def scrape(js:JsValue):List[GameData] = {
    NcaaComGameScraper.getGames(js) match {
      case Success(jsa) =>
        jsa.value.toList.flatMap(v => NcaaComGameScraper.getGameData(v, date.toString))
      case Failure(ex) =>
        logger.error("Failed scraping data ", ex)
        List.empty
    }
  }
}