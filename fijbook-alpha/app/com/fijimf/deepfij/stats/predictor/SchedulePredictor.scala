package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SchedulePredictor {
  val logger = Logger(this.getClass)
  val dao: ScheduleDAO
  val key: String

  def predictSchedule(sch: Schedule): Future[List[Int]] = {
    Future.sequence(sch.games.groupBy(_.date).map { case (date: LocalDate, games: List[Game]) => {
      logger.info("Handling date " + date)
      predictAndSaveDate(games, date, sch)
    }
    }).map(_.flatten.toList)
  }

  def gamesForDate(sch: Schedule, d: LocalDate) = {
    sch.gameResults.filter(_._1.date == d)
  }

  def gamesForTeam(sch: Schedule, k: String) = {
    sch.keyTeam.get(k) match {
      case Some(t) => sch.gameResults.filter(gr => gr._1.homeTeamId == t.id || gr._1.awayTeamId == t.id)
      case None => List.empty[(Game, Option[Result])]
    }
  }


  def predictDate(sch: Schedule, d: LocalDate): List[GamePrediction]

  def predictDates(sch: Schedule, from: LocalDate, to: LocalDate): List[(LocalDate, List[GamePrediction])] = {
    logger.info(s"Generating predictions for dates in $from to $to")
    Stream.iterate(from)(_.plusDays(1)).takeWhile(!_.isAfter(to)).map(d => {
      d -> predictDate(sch, d)
    }).toList
  }

  def predictTeam(sch: Schedule, key: String): List[GamePrediction]

  def predictTeams(sch: Schedule, keys: List[String]): List[(Team, List[GamePrediction])] = {
    if (keys.nonEmpty) logger.info(s"Generating predictions for (${keys.mkString(", ")})")
    keys.flatMap(k => {
      val maybeTeam = sch.keyTeam.get(k)
      maybeTeam.map(t => t -> predictTeam(sch, k))
    })
  }

  def predictAndSaveDate(games: List[Game], d: LocalDate, sch: Schedule): Future[List[Int]] = {
    val predictions: Future[List[GamePrediction]] = dao.loadGamePredictions(games, key).map(ops => {
      logger.info(s"Loaded ${ops.size} old predictions")
      val idMap = ops.map(op => op.gameId -> op.id).toMap
      val nps = predictDate(sch, d)
      logger.info(s"Calculated ${nps.size} new predictions")
      nps.map((np: GamePrediction) => np.copy(id = idMap.getOrElse(np.gameId, 0L)))
    })
    predictions.flatMap(dao.saveGamePredictions)
  }
}
