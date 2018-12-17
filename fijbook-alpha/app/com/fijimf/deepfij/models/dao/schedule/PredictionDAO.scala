package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{XPrediction, XPredictionModel}

import scala.concurrent.Future

trait PredictionDAO {
  def loadLatestPredictionModel(key: String): Future[Option[XPredictionModel]]

  def saveXPredictionModel(model: XPredictionModel) : Future[XPredictionModel]

  def updateXPredictions(modelId:Long, schedHash:String,xps: List[XPrediction]):Future[List[XPrediction]]

  def findXPredicitions(modelId:Long):Future[List[XPrediction]]
}
