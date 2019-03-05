package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{XPrediction, XPredictionModel}

import scala.concurrent.Future
import cats.data.OptionT
import cats.implicits._
import com.fijimf.deepfij.models.nstats.predictors.PredictionResult

trait PredictionDAO {
  def loadLatestPredictionModel(key: String): OptionT[Future,XPredictionModel]

  def loadPredictionModel(key: String, version:Int): OptionT[Future,XPredictionModel]

  def loadPredictionModels(key: String): Future[List[XPredictionModel]]

  def savePredictionModel(model: XPredictionModel) : OptionT[Future,XPredictionModel]

  def updatePredictions(modelId:Long, schedHash:String, xps: List[XPrediction]):Future[List[XPrediction]]

  def findXPredictions(modelId:Long):Future[List[XPrediction]]

  def findAllPredictions(): Future[Map[Long, List[PredictionResult]]]

}
