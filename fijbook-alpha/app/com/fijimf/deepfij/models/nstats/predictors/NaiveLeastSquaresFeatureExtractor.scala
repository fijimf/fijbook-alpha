package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.{Game, Schedule}

import scala.concurrent.Future

case class NaiveLeastSquaresFeatureExtractor(statDao: StatValueDAO) extends FeatureExtractor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val sfe = StatisticFeatureExtractor(statDao, List(("ols", "zscore")))

  def apply(gs: List[Game]): Future[List[Map[String, Double]]] = sfe(gs).map(lst => lst.map(m => transform(m)))

  def transform(m: Map[String, Double]): Map[String, Double] = {
    (for {
      h <- m.get("ols.value.home")
      a <- m.get("ols.value.away")
    } yield {
      Map("ols.value.diff" -> (h - a))
    }).getOrElse(Map.empty[String, Double])
  }
}
