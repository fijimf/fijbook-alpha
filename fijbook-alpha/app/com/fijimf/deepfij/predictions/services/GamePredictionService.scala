package com.fijimf.deepfij.predictions.services

import com.fijimf.deepfij.models.XPrediction

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait GamePredictionService {


  def update(year: Int, key:String, timeout: FiniteDuration): Future[List[XPrediction]]

}
