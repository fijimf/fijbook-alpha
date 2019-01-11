package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.XPrediction
import com.fijimf.deepfij.models.nstats.Analysis

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps


trait GamePredictionService {


  def update(year: Int, key:String, timeout: FiniteDuration): Future[List[XPrediction]]

}