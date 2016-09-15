package com.fijimf.deepfij.scraping

import java.time.{ZoneId, LocalDateTime}
import java.util

import org.scalatest.FlatSpec
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.io.Source
import scala.util.Success

class NcaaComGameScraperSpec extends FlatSpec {
  val isNov15 = classOf[NcaaComGameScraperSpec].getResourceAsStream("/test-data/nov15.txt")
  private val nov15 = Source.fromInputStream(isNov15).mkString
  val scraper = new NcaaComGameScraper {}
  "NcaaComGameScraper" should "parse the JSON in " in {
    val jsValue: JsValue = Json.parse(scraper.stripCallbackWrapper(nov15))
    assert((jsValue \ "scoreboard").asOpt[JsArray].isDefined)
  }

  it should "extract an array of Json game data " in {
    val jsValue: JsValue = Json.parse(scraper.stripCallbackWrapper(nov15))
    assert(scraper.getGames(jsValue).isSuccess)
  }

  it should "pull locations from games" in {
    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
      case Success(js) => assert(js.value.flatMap(scraper.gameLocation) == locations)
      case _ => fail()
    }
  }

  it should "pull start date time from games" in {
    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
      case Success(js) => assert(js.value.flatMap(scraper.gameStartTime) == startTimes.map(LocalDateTime.parse(_)))
      case _ => fail()
    }
  }

  it should "pull final status" in {
    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
      case Success(js) => assert(js.value.forall(value => scraper.isGameFinal(value).getOrElse(false)))
      case _ => fail()
    }
  }
  it should "pull home team " in {
    scraper.getGames(Json.parse(scraper.stripCallbackWrapper(nov15))) match {
      case Success(js) =>
        js.value.foreach(gg=> println(scraper.gameHomeTeam(gg)))
      case _ => fail()
    }
  }

  val startTimes: Seq[String] = Seq(
    "2015-11-15T12:00",
    "2015-11-15T13:00",
    "2015-11-15T13:00",
    "2015-11-15T13:30",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:00",
    "2015-11-15T14:30",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T15:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:00",
    "2015-11-15T16:30",
    "2015-11-15T16:30",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:00",
    "2015-11-15T17:30",
    "2015-11-15T18:00",
    "2015-11-15T18:00",
    "2015-11-15T18:00",
    "2015-11-15T18:30",
    "2015-11-15T19:00",
    "2015-11-15T19:00",
    "2015-11-15T19:00",
    "2015-11-15T20:00",
    "2015-11-15T20:00",
    "2015-11-15T20:00",
    "2015-11-15T21:00",
    "2015-11-15T21:30",
    "2015-11-15T22:00"
  )

  val locations: Seq[String] = Seq(
    "Value City Arena at the Jerome Schottenstein Center, Columbus, OH",
    "Louis Brown Athletic Center, Piscataway, NJ",
    "Sojka Pavilion, Lewisburg, PA",
    "Walsh Gymnasium, South Orange, NJ",
    "Mackey Arena, West Lafayette, IN",
    "Binghamton University Events Center, Binghamton, NY",
    "Littlejohn Coliseum, Clemson, SC",
    "Kirby Sports Center, Easton, PA",
    "NIU Convocation Center, DeKalb, IL",
    "Fifth Third Arena, Cincinnati, OH",
    "Athletics-Recreation Center, Valparaiso, IN",
    "Williams Arena, Minneapolis, MN",
    "Donald L. Tucker Center, Tallahassee, FL",
    "Prairie Capital Convention Center, Springfield, IL",
    "Pete Mathews Coliseum, Jacksonville, AL",
    "Trojan Arena, Troy, AL",
    "Ford Center, Evansville, IN",
    "Leavey Center, Santa Clara, CA",
    "Palestra, Philadelphia, PA",
    "Millis Athletic Convocation Center, High Point, NC",
    "SIU Arena, Carbondale, IL",
    "Dean Smith Center, Chapel Hill, NC",
    "Ramsey Center, Cullowhee, NC",
    "NIU Convocation Center, DeKalb, IL",
    "Chaifetz Arena, St. Louis, MO",
    "KeyArena, Seattle, WA",
    "Baxter Arena, Omaha, NE",
    "Carver-Hawkeye Arena, Iowa City, IA",
    "Robins Center, Richmond, VA",
    "Leavey Center, Santa Clara, CA",
    "Western Hall, Macomb, IL",
    "Mizzou Arena, Columbia, MO",
    "PNC Arena, Raleigh, NC",
    "Reese Court, Cheney, WA",
    "Municipal Auditorium, Kansas City, MO",
    "Stan Sheriff Center, Honolulu, HI",
    "The Pavilion, Davis, CA",
    "Kohl Center, Madison, WI",
    "Bren Events Center, Irvine, CA",
    "Maples Pavilion, Stanford, CA",
    "Pan American Center, Las Cruces, NM",
    "Stan Sheriff Center, Honolulu, HI",
    "Pauley Pavilion, Los Angeles, CA")
}

