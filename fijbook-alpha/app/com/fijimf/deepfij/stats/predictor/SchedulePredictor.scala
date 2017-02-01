package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, GamePrediction, Schedule, ScheduleDAO}
import play.api.Logger

import scala.collection.immutable.Iterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SchedulePredictor {
  val logger = Logger(this.getClass)
  val dao: ScheduleDAO
  val key: String

  def predictSchedule(sch: Schedule): Future[List[Int]] = {
    Future.sequence(sch.games.groupBy(_.date).map { case (date: LocalDate, games: List[Game]) => {
      logger.info("Handling date "+date)
      predictAndSaveDate(games, date, sch)
    } }).map(_.flatten.toList)
  }

  def predictDate(gs: List[Game], d: LocalDate, sch: Schedule): Future[List[GamePrediction]]

  def predictAndSaveDate(games: List[Game], d: LocalDate, sch: Schedule): Future[List[Int]] = {
    val predictions: Future[List[GamePrediction]] = dao.loadGamePredictions(games, key).flatMap(ops => {
      logger.info(s"Loaded ${ops.size} old predictions")
      predictDate(games, d, sch).map(nps => {
        logger.info(s"Calculated ${nps.size} new predictions")

        val idMap = ops.map(op => op.gameId -> op.id).toMap
        nps.map((np: GamePrediction) => np.copy(id = idMap.getOrElse(np.gameId, 0L)))
      })
    })
    predictions.flatMap(dao.saveGamePredictions)
  }
}
