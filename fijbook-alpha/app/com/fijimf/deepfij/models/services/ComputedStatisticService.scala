package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.nstats.Analysis
import com.fijimf.deepfij.stats.{Model, Stat}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps


trait ComputedStatisticService {
  val models: List[Analysis[_]]

  def update(year: Int, timeout: FiniteDuration): Future[String]

}