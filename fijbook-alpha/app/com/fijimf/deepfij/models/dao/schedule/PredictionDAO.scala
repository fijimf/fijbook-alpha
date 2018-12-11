package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{XPredictionModel}

import scala.concurrent.Future

trait PredictionDAO {
  def loadLatestPredictionModel(key: String): Future[Option[XPredictionModel]]

  def saveXPredictionModel(model: XPredictionModel) : Future[XPredictionModel]

}
