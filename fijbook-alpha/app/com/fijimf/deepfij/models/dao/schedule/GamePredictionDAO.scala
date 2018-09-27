package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, GamePrediction, LogisticModelParameter, StatValue}

import scala.concurrent.Future

trait GamePredictionDAO {

  def listGamePrediction: Future[List[GamePrediction]]

  def loadGamePredictions(games: List[Game], modelKey: String): Future[List[GamePrediction]]

  def saveGamePredictions(gps: List[GamePrediction]): Future[List[GamePrediction]]

}
