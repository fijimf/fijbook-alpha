package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.stats.predictor.SchedulePredictor

import scala.language.postfixOps


trait GamePredictorService {
  val models: List[ScheduleDAO=>SchedulePredictor]

  def update()

}