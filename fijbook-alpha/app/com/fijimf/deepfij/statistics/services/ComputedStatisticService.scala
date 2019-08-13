package com.fijimf.deepfij.statistics.services

import com.fijimf.deepfij.models.nstats.Analysis

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait ComputedStatisticService {
  val models: List[Analysis[_]]

  def update(year: Int, timeout: FiniteDuration): Future[String]

}
