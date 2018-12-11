package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.{XPredictionModel, XPredictionModelParameter}

import scala.concurrent.Future

trait PredictionDAO {
  def loadLatestPredictionModel(key: String): Future[Option[XPredictionModel]]

  def loadParametersForModel(modelId: Long): Future[List[XPredictionModelParameter]]

  def saveXPredictionModel(model: XPredictionModel) : Future[XPredictionModel]

  def saveXPredictionModelParameters(parameters: List[XPredictionModelParameter]) : Future[List[XPredictionModelParameter]]

}
