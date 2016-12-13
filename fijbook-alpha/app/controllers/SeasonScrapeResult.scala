package controllers

import java.time.LocalDate

/**
  * Created by jimfrohnhofer on 12/1/16.
  */
object SeasonScrapeResult {
  def apply(list: List[(LocalDate, GameScrapeResult)]): SeasonScrapeResult = {
    val gameCounts: Map[LocalDate, Int] = list.map(tup => tup._1 -> tup._2.ids.size).toMap
    val unmappedTeamCount: Map[String, Int] = list.flatMap(_._2.unmappedKeys).groupBy(_.toString).map(tup => (tup._1, tup._2.size))
    SeasonScrapeResult(gameCounts, unmappedTeamCount)
  }
}

case class SeasonScrapeResult(gameCounts: Map[LocalDate, Int], unmappedTeamCount: Map[String, Int])