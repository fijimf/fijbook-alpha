package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.{Game, Result}

import scala.concurrent.Future
import scala.util.Random

trait CategoryExtractor {

  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(grs: List[(Game, Result)]): Future[List[(Long,Option[Double])]] = {
    Future.sequence(grs.map { case (g, r) => apply(g, r) })
  }

  def apply(g: Game, r: Result): Future[(Long,Option[Double])] = Future.successful(0L->None)
}

object CategoryExtractor {

  import scala.concurrent.ExecutionContext.Implicits.global

  def repeat(c: CategoryExtractor, n: Int): CategoryExtractor = {
    new CategoryExtractor {
      override def apply(grs: List[(Game, Result)]): Future[List[(Long,Option[Double])]] = {
        Future.sequence(List.fill(n)(c(grs))).map(_.flatten)
      }
    }
  }

  def stitch(cs: CategoryExtractor*): CategoryExtractor = {
    new CategoryExtractor {
      override def apply(grs: List[(Game, Result)]): Future[List[(Long,Option[Double])]] = {
        Future.sequence(cs.map(c => c(grs)).toList).map(_.flatten)
      }
    }
  }
}

case class WinLossCategoryExtractor(winVal: Double = 1.0, lossVal: Double = 0.0, tieVal: Option[Double] = None) extends CategoryExtractor {
  override def apply(g: Game, r: Result): Future[(Long,Option[Double])] = {
    Future.successful(
      g.id->( if (r.homeScore > r.awayScore)
        Some(winVal)
      else if (r.homeScore < r.awayScore)
        Some(lossVal)
      else
        tieVal)
    )
  }
}

case class SpreadCategoryExtractor(preTransform: Result => Option[Result], postTransform: Double => Option[Double]) extends CategoryExtractor {
  override def apply(g: Game, r: Result): Future[(Long,Option[Double])] = {
    Future.successful(g.id->(for {
      r1 <- preTransform(r)
      c <- postTransform(r1.homeScore - r1.awayScore)
    } yield c))
  }
}


object SpreadTransformers {
  val rng = new Random()
  val id: Double => Option[Double] = {
    x => Some(x)
  }

  def bucket(span: Int): Double => Option[Double] = {
    require(span % 2 != 0 && span > 2)
    x: Double => {
      Some(if (x > span / 2) {
        math.round(x / span).toDouble
      } else if (x < -span / 2) {
        math.round(x / span).toDouble
      } else {
        0.0
      })
    }
  }

  def cap(max: Double): Double => Option[Double] = {
    require(max > 0.0)
    x =>
      Some(
        if (x > max) {
          max
        } else if (x < -max) {
          -max
        } else {
          x
        }
      )
  }

  def trunc(max: Double): Double => Option[Double] = {
    require(max > 0.0)
    x =>
      if (x > max || x < -max) {
        None
      } else {
        Some(x)
      }
  }

  /**
    *
    * @param sd standard deviation
    * @return a function which randomly scales a number by 1.0 + N(0,sd).
    *         For reference, 99.7% of the values will be scaled between 1.0 +/- 3*sd.  For example, with a value of 0.1
    *         we would expect 99.7% of the values to be scaled by values between [.7 , 1.3]
    */
  val handleTies: Result => Option[Result] = {
    r => {
      if (r.periods > 2) {
        val s = math.min(r.homeScore, r.awayScore)
        Some(r.copy(homeScore = s, awayScore = s))
      } else {
        Some(r)
      }
    }
  }

  val dropTies: Result => Option[Result] = {
    r => {
      if (r.periods > 2) {
        None
      } else {
        Some(r)
      }
    }
  }

  def noisy(sd: Double): Result => Option[Result] = {
    r =>
      Some(r.copy(
        homeScore = math.round(r.homeScore * (1.0 + rng.nextGaussian() * sd)).toInt,
        awayScore = math.round(r.awayScore * (1.0 + rng.nextGaussian() * sd)).toInt
      ))
  }

  def handleTiesWithNoise(sd: Double): Result => Option[Result] = {
    val n = noisy(sd)
    r =>
      for {
        s <- handleTies(r)
        t <- n(s)
      } yield t
  }
}
