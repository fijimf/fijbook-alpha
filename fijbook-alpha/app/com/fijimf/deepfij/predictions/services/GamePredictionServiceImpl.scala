package com.fijimf.deepfij.predictions.services

import com.fijimf.deepfij.models.XPrediction
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.predictors.PredictorContext
import javax.inject.Inject
import play.api.{Configuration, Logger}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class GamePredictionServiceImpl @Inject()(dao: ScheduleDAO, cfg:Configuration) extends GamePredictionService {

  val logger = Logger(this.getClass)

  private val predCtx: PredictorContext = PredictorContext(dao)

  override def update(year: Int, key:String, timeout: FiniteDuration): Future[List[XPrediction]] = {
   Future.successful(List.empty[XPrediction]) //TODO
  }

}

