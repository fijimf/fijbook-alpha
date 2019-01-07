package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction}

import scala.concurrent.Future

trait ModelEngine[+M<: java.io.Serializable] {
  val kernel: Option[M]

  def train(s: List[Schedule], dx: StatValueDAO): Future[ModelEngine[M]]

  def predict(s: Schedule, ss: StatValueDAO): List[Game] => Future[List[Option[XPrediction]]]
}
