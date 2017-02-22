package com.fijimf.deepfij.models
import java.time.LocalDate

import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait AnalyticsDAO {

  def listStatValues: Future[List[StatValue]]

  def listLogisticModel: Future[List[LogisticModelParameter]]

  def listGamePrediction: Future[List[GamePrediction]]

  def loadGamePredictions(games: List[Game], modelKey: String): Future[List[GamePrediction]]

  def saveGamePredictions(gps: List[GamePrediction]): Future[List[Int]]

  def deleteStatValues(dates: List[LocalDate], model: List[String]): Future[Unit]

  def saveStatValues(batchSize: Int, dates: List[LocalDate], model: List[String], stats: List[StatValue]): Unit

  def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]]

  def loadStatValues(modelKey: String): Future[List[StatValue]]
}
