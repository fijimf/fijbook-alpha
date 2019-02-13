package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{XPrediction, XPredictionModel}

import scala.concurrent.Future

trait PredictionDAO {
  def loadLatestPredictionModel(key: String): Future[XPredictionModel]

  def loadPredictionModel(key: String, version:Int): Future[Option[XPredictionModel]]

  def loadPredictionModel(key: String): Future[List[XPredictionModel]]

  def savePredictionModel(model: XPredictionModel) : Future[XPredictionModel]

  def updatePredictions(modelId:Long, schedHash:String, xps: List[XPrediction]):Future[List[XPrediction]]

  def findXPredicitions(modelId:Long):Future[List[XPrediction]]
}
