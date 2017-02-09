package com.fijimf.deepfij.scraping

import java.time.LocalDate

import com.fijimf.deepfij.scraping.modules.scraping.model.GameData
import play.api.libs.json.JsValue

import scala.util.{Failure, Success}

case class ScoreboardByDateReq(date:LocalDate) extends JsonScrapeRequest[List[GameData]] with NcaaComGameScraper {
  override def url = "http://data.ncaa.com/jsonp/scoreboard/basketball-men/d1/%04d/%02d/%02d/scoreboard.html".format(date.getYear, date.getMonthValue ,date.getDayOfMonth )
  override def preProcessBody(s:String) = stripCallbackWrapper(s)
  override def scrape(js:JsValue):List[GameData] = {
    getGames(js) match {
      case Success(jsa) =>
        jsa.value.toList.flatMap(v=>getGameData(v, date.toString))
      case Failure(ex) =>
        logger.error("Failed scraping data ", ex)
        List.empty
    }
  }
}