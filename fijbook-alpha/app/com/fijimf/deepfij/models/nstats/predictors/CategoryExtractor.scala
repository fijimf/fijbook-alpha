package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.{Game, Result}

import scala.concurrent.Future
import scala.util.Random

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
  val rng = new Random()
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

  /**
    *
    * @param sd standard deviation
    * @return a function which randomly scales a number by 1.0 + N(0,sd).
    *         For reference, 99.7% of the values will be scaled between 1.0 +/- 3*sd.  Fore example, with a value of 0.1
    *         we would expect 99.7% of the values to be scaled by values between [.7 , 1.3]
    */
  def noisy(sd: Double): Double => Double = {
    x => x * (1.0 + rng.nextGaussian() / sd)
  }

  def cappedNoisy(max:Double, sd:Double):Double=>Double={
    val f = cap(max)
    val g = noisy(sd)
    x=>f(g(x))
  }
}