package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.{Game, Result}

import scala.concurrent.Future

trait CategoryExtractor {
  def apply(grs: List[(Game, Result)]): Future[List[Option[Double]]]
}


case class WinLossCategoryExtractor(winVal: Double = 1.0, lossVal: Double = 0.0, tieVal: Option[Double] = None) extends CategoryExtractor {
  override def apply(grs: List[(Game, Result)]): Future[List[Option[Double]]] = {
    Future.successful(grs.map { case (_, r) =>
      if (r.homeScore > r.awayScore)
        Some(winVal)
      else if (r.homeScore < r.awayScore)
        Some(lossVal)
      else
        tieVal
    })

  }
}

case class SpreadCategoryExtractor(transform: Double => Double = SpreadTransformers.id, ignoreExtraPeriods: Boolean = true) extends CategoryExtractor {
  override def apply(grs: List[(Game, Result)]): Future[List[Option[Double]]] = {
    Future.successful(grs.map { case (_, r) =>
      if (r.periods > 2) {
        Some(transform(0.0))
      } else {
        Some(transform(r.homeScore - r.awayScore))
      }
    })
  }
}


object SpreadTransformers {
  val id: Double => Double = {
    x => x
  }

  def cap(max: Double): Double => Double = {
    require(max > 0.0)
    x =>
      if (x > max) {
        max
      } else if (x < -max) {
        -max
      } else {
        x
      }
  }
}