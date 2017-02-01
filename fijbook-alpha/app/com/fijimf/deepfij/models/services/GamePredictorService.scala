package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.stats.predictor.SchedulePredictor

import scala.language.postfixOps


trait GamePredictorService {
  val models: List[SchedulePredictor]

  def update()

}