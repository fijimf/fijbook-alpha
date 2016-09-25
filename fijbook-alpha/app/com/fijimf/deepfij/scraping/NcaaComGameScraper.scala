package com.fijimf.deepfij.scraping

import java.time._
import java.util.TimeZone
import java.util.spi.TimeZoneNameProvider
import play.api.Logger
import play.api.libs.json._

import scala.util.{Failure, Success, Try}


trait NcaaComGameScraper {
  val logger: Logger = Logger(this.getClass)
  val zoneId: ZoneId = ZoneId.of("America/New_York")

  def getGames(v: JsValue): Try[JsArray] = ((v \ "scoreboard") (0) \ "games").validate[JsArray] match {
    case JsSuccess(value, _) => Success(value)
    case e: JsError =>
      val message: String = "Errors: " + JsError.toJson(e).toString()
      logger.error(message)
      Failure(new IllegalArgumentException(message))
  }

  def gameLocation(v: JsValue): Option[String] = (v \ "location").validate[String] match {
    case JsSuccess(value, _) => Some(value)
    case e: JsError =>
      val message: String = "Failed extracting location Errors: " + JsError.toJson(e).toString()
      logger.warn(message)
      None
  }

  def gameHomeTeam(v: JsValue) = gameTeam(v, "home")

  def gameAwayTeam(v: JsValue) = gameTeam(v, "away")

  def gameTeam(v: JsValue, homeAway: String): List[String] = (v \ homeAway).validate[JsValue] match {
    case JsSuccess(value, _) => {
      val iconUrl = (value \ "iconURL").validate[String] match {
        case JsSuccess(icu, _) =>
          val s1 = icu.substring(icu.lastIndexOf('/') + 1)
          val s2 = s1.substring(0, s1.indexOf('.'))
          Some(s2)
        case e: JsError =>
          val message: String = "Failed extracting iconURL Errors: " + JsError.toJson(e).toString()
          logger.warn(message)
          None
      }
      val schoolUrl = (value \ "name").validate[String] match {
        case JsSuccess(n, _) =>
          if (n.length > 0) {
            val s1 = n.substring(n.indexOf('\'') + 1)
            val s2 = s1.substring(0, s1.indexOf('\''))
            Some(s2.replace("/schools/", ""))
          } else {
            None
          }
        case e: JsError => val message: String = "Failed extracting schoolUrl Errors: " + JsError.toJson(e).toString()
          logger.warn(message)
          None
      }
      val schoolHtmlName = (value \ "name").validate[String] match {
        case JsSuccess(n, _) =>
          if (n.length > 0) {
            val s1 = n.substring(n.indexOf('>') + 1)
            val s2 = s1.substring(0, s1.indexOf('<'))
            Some(s2)
          } else {
            None
          }
        case e: JsError => None
      }

      val nameRaw = (value \ "nameRaw").validate[String] match {
        case JsSuccess(n, _) => Some(n)
        case e: JsError => None
      }
      val nameSeo = (value \ "nameSeo").validate[String] match {
        case JsSuccess(n, _) => Some(n)
        case e: JsError => None
      }
      val shortName = (value \ "shortname").validate[String] match {
        case JsSuccess(n, _) => Some(n)
        case e: JsError => None
      }
      List(schoolUrl, iconUrl, schoolHtmlName, nameSeo, nameRaw, shortName).flatten
    }
    case e: JsError =>
      val message: String = "Failed extracting location Errors: " + JsError.toJson(e).toString()
      logger.warn(message)
      List.empty[String]
  }

  def gameStartTime(v: JsValue): Option[LocalDateTime] = (v \ "startTimeEpoch").validate[String] match {
    case JsSuccess(value, _) =>
      Try(value.toLong) match {
        case Success(i) =>
          Some(Instant.ofEpochMilli(i * 1000).atZone(zoneId).toLocalDateTime)
        case Failure(ex) =>
          logger.warn("Error parsing epochStartTime:" + value)
          None
      }
    case e: JsError =>
      val message: String = "Failed extracting location Errors: " + JsError.toJson(e).toString()
      logger.warn(message)
      None

  }

  def isGameFinal(v: JsValue): Option[Boolean] = (v \ "gameState").validate[String] match {
    case JsSuccess(value, _) => Some(value.equalsIgnoreCase("final"))
    case e: JsError =>
      val message: String = "Failed extracting gameState Errors: " + JsError.toJson(e).toString()
      logger.warn(message)
      None
  }


  //  def getGameData(v: JsValue): Option[GameData] = {
  //    val optResult = for (
  //      gs <- (v \ "gameState").asOpt[String] if gs.equalsIgnoreCase("final");
  //      ps <- (v \ "scoreBreakdown").asOpt[JsArray];
  //      hs <- (v \ "home" \ "currentScore").asOpt[String];
  //      as <- (v \ "away" \ "currentScore").asOpt[String]
  //    ) yield {
  //      Result(hs.toInt, as.toInt, ps.value.size)
  //    }
  //
  //    val optTourneyInfo = for (
  //      ti <- (v \ "tournament_d").asOpt[String];
  //      rg <- (v \ "bracket_region").asOpt[String];
  //      hs <- (v \ "home" \ "team_seed").asOpt[String];
  //      as <- (v \ "away" \ "team_seed").asOpt[String]
  //    ) yield {
  //      TourneyInfo(rg, hs.toInt, as.toInt)
  //    }
  //
  //    for (
  //      sd <- (v \ "startDate").asOpt[String];
  //      cn <- (v \ "conference").asOpt[String];
  //      ht <- (v \ "home" \ "name").asOpt[String];
  //      hk <- pullKeyFromLink(ht);
  //      at <- (v \ "away" \ "name").asOpt[String];
  //      ak <- pullKeyFromLink(at)
  //    ) yield {
  //      GameData(new LocalDate(sd), hk, ak, optResult, (v \ "location").asOpt[String], optTourneyInfo, cn)
  //    }
  //  }

  def pullKeyFromLink(s: String): Option[String] = {
    val regex = """'/schools/([\w\-]+)'""".r.unanchored
    s match {
      case regex(key) => Some(key)
      case _ => None
    }
  }

  def stripCallbackWrapper(json: String): String = {
    json
      .replaceFirst( """^callbackWrapper\(\{""", """{""")
      .replaceFirst( """}\);$""", """}""")
      .replaceAll( """,\s+,""", ", ")
      .replaceAll( """,\s+,\s+,""", ", ")
      .replaceAll( """,\s+,\s+,\s+,""", ", ")
      .replaceAll( """,\s+,\s+,\s+,\s+,""", ", ")
      .replaceAll( """,\s+,\s+,\s+,\s+,\s+,""", ", ")
      .replaceAll( """,\s+,\s+,\s+,\s+,\s+,\s+,""", ", ")
      .replaceAll( """\[\s+,""", "[ ")
      .replaceAll( """,\s+\]""", "] ")

  }
}
