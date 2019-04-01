package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.Game
import com.fijimf.deepfij.models.dao.schedule.StatValueDAO

import scala.concurrent.Future


case class BaseLogisticFeatureExtractor(statDao: StatValueDAO) extends FeatureExtractor {
  import scala.concurrent.ExecutionContext.Implicits.global

//  val sfe = StatisticFeatureExtractor(statDao, List(("ols", "zscore"), ("mean-margin","zscore"), ("variance-margin","percentile")))
  val sfe = StatisticFeatureExtractor(statDao, List(("ols", "value")))

  def apply(gs: List[Game]): Future[List[(Long,Map[String, Double])]] = sfe(gs).map(lst => lst.map(m => m._1->transform(m._2)))

  def transform(m: Map[String, Double]): Map[String, Double] = {
    (for {
      h <- m.get("ols.value.home")
      a <- m.get("ols.value.away")
//      hm <- m.get("mean-margin.zscore.home")
//      am <- m.get("mean-margin.zscore.away")
//      hv <- m.get("variance-margin.percentile.home")
//      av <- m.get("variance-margin.percentile.away")
    } yield {
      Map(
        "ols.value.home" -> h,
        "ols.value.away" -> a,
        "ols.value.diff" -> (h - a)  //,
//        "mean-margin.zscore.home" -> hm,
//        "mean-margin.zscore.away" -> am,
//        "mean-margin.zscore.diff" -> (hm - am),
//        "variance-margin.percentile.home" -> hv,
//        "variance-margin.percentile.away" -> av,
//        "variance-margin.percentile.diff" -> (hv-av),
//        "variance-margin.percentile.sum" -> (hv+av)
      )
    }).getOrElse(Map.empty[String, Double])
  }
}
