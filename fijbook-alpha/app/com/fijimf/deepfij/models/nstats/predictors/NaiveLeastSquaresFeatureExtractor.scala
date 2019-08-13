package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.StatValueDAO
import com.fijimf.deepfij.models.Game

import scala.concurrent.Future

case class NaiveLeastSquaresFeatureExtractor(statDao: StatValueDAO) extends FeatureExtractor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val sfe = StatisticFeatureExtractor(statDao, List(("ols", "value")))

  def apply(gs: List[Game]): Future[List[(Long, Map[String, Double])]] = sfe(gs).map(lst => lst.map(m => transform(m)))

  def transform(m: (Long,Map[String, Double])): (Long,Map[String, Double]) = {
    m._1-> (for {
      h <- m._2.get("ols.value.home")
      a <- m._2.get("ols.value.away")
    } yield {
      Map("ols.value.diff" -> (h - a))
    }).getOrElse(Map.empty[String, Double])
  }
}
