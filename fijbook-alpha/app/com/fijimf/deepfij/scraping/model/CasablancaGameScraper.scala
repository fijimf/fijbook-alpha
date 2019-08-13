package com.fijimf.deepfij.scraping.model

import java.time.{Instant, LocalDateTime, ZoneId}

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, JsValue}

import scala.util.Try

object CasablancaGameScraper {
  val logger: Logger = Logger(this.getClass)
  val zoneId: ZoneId = ZoneId.of("America/New_York")
  val requestPath: String = "https://data.ncaa.com/casablanca/scoreboard/basketball-men/d1/%04d/%02d/%02d/scoreboard.json"
  val mapper = new ObjectMapper()
  mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)

  def getGames(s: String): Try[List[JsonNode]] = Try {
    import scala.collection.JavaConverters._
    mapper.readTree(s).at("/games").elements().asScala.toList
  }

  def gameLocation(v: JsValue): Option[String] = None

  def gameStartTime(v: JsonNode): Option[LocalDateTime] =
    longNode(v, "/game/startTimeEpoch").map(i => Instant.ofEpochMilli(i * 1000).atZone(zoneId).toLocalDateTime)

  def isGameFinal(v: JsValue): Option[Boolean] = (v \ "gameState").validate[String] match {
    case JsSuccess(value, _) => Some(value.equalsIgnoreCase("final"))
    case e: JsError =>
      val message: String = "Failed extracting gameState Errors: " + JsError.toJson(e).toString()
      logger.warn(message)
      None
  }

  def textNode(n: JsonNode, ptr: String): Option[String] = {
    val node = n.at(ptr)
    if (node.isMissingNode || node.isNull || !node.isValueNode) None
    else Some(node.asText())
  }

  def parsePeriod(n: JsonNode, ptr: String): Option[Int] = {
    val node = n.at(ptr)
    if (node.isMissingNode || node.isNull || !node.isValueNode) None
    else {
      val cp = node.asText().toLowerCase()
      if (cp.contains("ot")) {
        if (cp.contains("2ot")) {
          Some(4)
        } else if (cp.contains("3ot")) {
          Some(5)
        } else if (cp.contains("4ot")) {
          Some(6)
        } else if (cp.contains("5ot")) {
          Some(7)
        } else {
          Some(3)
        }
      } else {
        Some(2)
      }
    }
  }

  def intNode(n: JsonNode, ptr: String): Option[Int] = {
    val node = n.at(ptr)
    if (node.isMissingNode || node.isNull || !node.isValueNode) {
      None
    } else {
      if (node.isIntegralNumber)
        Some(node.asInt())
      else
        Try {
          node.asText().toInt
        }.toOption
    }
  }

  def longNode(n: JsonNode, ptr: String): Option[Long] = {
    val node = n.at(ptr)
    if (node.isMissingNode || node.isNull || !node.isValueNode) {
      None
    } else {
      if (node.isIntegralNumber)
        Some(node.asLong())
      else
        Try {
          node.asText().toLong
        }.toOption
    }

  }


  def getGameData(v: JsonNode, sourceKey: String): Option[GameData] = {
    val optResult = for {
      gs <- textNode(v, "/game/gameState") if gs.equalsIgnoreCase("final")
      ps <- parsePeriod(v, "/game/currentPeriod")
      hs <- intNode(v, "/game/home/score")
      as <- intNode(v, "/game/away/score")
    } yield {
      ResultData(hs, as, ps)
    }

    for {
      sd <- gameStartTime(v)
      hk <- textNode(v, "/game/home/names/seo")
      ak <- textNode(v, "/game/away/names/seo")
    } yield {
      GameData(sd, hk, ak, optResult, None, None, "", sourceKey)
    }
  }

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
