package com.fijimf.deepfij.scraping

import java.time.LocalDate

import com.fijimf.deepfij.scraping.modules.scraping.model.GameData
import play.api.Logger
import play.api.libs.json.JsValue

import scala.util.{Failure, Success}

final case class ScoreboardByDateReq(date: LocalDate) extends JsonScrapeRequest[List[GameData]] {
  val logger = Logger(this.getClass)
  override def url: String = NcaaComGameScraper.requestPath.format( date.getYear, date.getMonthValue ,date.getDayOfMonth )
  override def preProcessBody(s: String): String = NcaaComGameScraper.stripCallbackWrapper(s)
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


final case class CasablancaScoreboardByDateReq(date: LocalDate) extends CasablancaJsonScrapeRequest[List[GameData]] {
  val logger = Logger(this.getClass)
  override def url: String = CasablancaGameScraper.requestPath.format( date.getYear, date.getMonthValue ,date.getDayOfMonth )
  override def preProcessBody(s: String): String = s
  override def scrape(js:String):List[GameData] = {
    CasablancaGameScraper.getGames(js) match {
      case Success(jsa) =>
        jsa.flatMap(v => CasablancaGameScraper.getGameData(v, date.toString))
      case Failure(ex) =>
        logger.error("Failed scraping data ", ex)
        List.empty
    }
  }
}