package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, XPrediction}
import com.fijimf.deepfij.schedule.model.Schedule

import scala.concurrent.Future

trait ModelEngine[T] {

  def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[T]]

  def predict(s: Schedule, ss: StatValueDAO): List[Game] => Future[List[XPrediction]]

  def toString: String

}
