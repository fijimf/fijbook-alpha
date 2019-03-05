package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.Game

import scala.concurrent.Future

trait FeatureExtractor {
  def apply(gs: List[Game]): Future[List[Map[String, Double]]]
}

object FeatureExtractor {

  import scala.concurrent.ExecutionContext.Implicits.global

  def repeat(f: FeatureExtractor, n: Int): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[Map[String, Double]]] = {
        f(gs).map(fm => List.fill(n)(fm).flatten)
      }
    }
  }

  def stitch(fs: FeatureExtractor*): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[Map[String, Double]]] = {
        Future.sequence(fs.map(f => f(gs))).map(fms => fms.flatten.toList)
      }
    }
  }

  def mergeAll(fs: FeatureExtractor*): FeatureExtractor = {
    fs.toList.reduce(merge)
  }

  def merge(a: FeatureExtractor, b: FeatureExtractor): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[Map[String, Double]]] = {
        for {
          x <- a(gs)
          y <- b(gs)
        } yield {
          x.zip(y).map(tup => tup._1 ++ tup._2)
        }
      }
    }
  }
}





