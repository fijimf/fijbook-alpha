package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.{Game, Schedule}
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.schedule.model.Schedule
import play.api.Logger

import scala.collection.immutable
import scala.concurrent.Future
import scala.io.Source

object TourneyUpdater {

  val logger = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  def updateNcaaTournamentGames(fileName: String, dao: ScheduleDAO): Future[List[Game]] = {
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream(fileName)).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val tourneyData: Map[Int, (LocalDate, Map[String, (String, Int)])] = lines.foldLeft(Map.empty[Int, (LocalDate, Map[String, (String, Int)])])((data: Map[Int, (LocalDate, Map[String, (String, Int)])], str: String) => {
      str.split(",").toList match {
        case year :: date :: Nil =>
          val y = year.toInt
          val d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          data + (y -> (d, Map.empty[String, (String, Int)]))
        case year :: region :: seed :: key :: Nil =>
          val y = year.toInt
          val s = seed.toInt
          data.get(y) match {
            case Some(mm) =>
              data + (y -> (mm._1, mm._2 + (key -> (region, s))))
            case None => data
          }
        case _ => data
      }
    })

    Future.sequence(tourneyData.map { case (y, (startDate, seedData)) => {
        dao.loadSchedule(y).flatMap {
          case Some(s) =>
            val gs = s.games.filterNot(g => g.date.isBefore(startDate))
            val updatedGames: List[Game] = gs.flatMap(g => {
              val hk = s.teamsMap(g.homeTeamId).key
              val ak = s.teamsMap(g.awayTeamId).key
              (seedData.get(hk).map(_._2), seedData.get(ak).map(_._2)) match {
                case (Some(hs), Some(as)) => Some(g.copy(tourneyKey=Some("ncaa"),homeTeamSeed = Some(hs), awayTeamSeed = Some(as)))
                case _ => None
              }
            })
            dao.updateGames(updatedGames)
          case None =>
            Future.successful(List.empty[Game])
        }
      }
    }.toList).map(_.flatten)
  }


  def updateConferenceTournamentGames(fileName: String, dao: ScheduleDAO): Future[List[Game]] = {
    val lines: List[String] = Source.fromInputStream(getClass.getResourceAsStream(fileName)).getLines.toList.map(_.trim).filterNot(_.startsWith("#")).filter(_.length > 0)
    val tourneyData: Map[Int, List[(String, LocalDate)]] = lines.flatMap { str =>
      str.split(",").toList match {
        case year :: date :: conference :: Nil =>
          val y = year.toInt
          val d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          Some((y, (conference,d)))
        case _ => None
      }
    }.groupBy(_._1).mapValues(_.map(_._2))

    Future.sequence(tourneyData.map { case (year, tourneys) => {
        dao.loadSchedule(year).flatMap {
          case Some(s) =>
            dao.updateGames(updateSchedule(s,tourneys))
          case None =>
            Future.successful(List.empty[Game])
        }
      }
    }).map(_.flatten.toList)
  }

  private def updateSchedule(s:Schedule, tourneys: List[(String, LocalDate)]): List[Game] = {
    import s._
    tourneys.flatMap { case (key, date) =>
      s.games.filter(_.isConferenceGame(key)).filterNot(_.date.isBefore(date)).map(_.copy(tourneyKey = Some(key))).toList
    }
  }
}
