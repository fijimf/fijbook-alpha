package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.{Game, Result}

import scala.concurrent.Future

trait FeatureExtractor {
  def apply(gs: List[Game]): Future[List[Map[String, Double]]]
}


