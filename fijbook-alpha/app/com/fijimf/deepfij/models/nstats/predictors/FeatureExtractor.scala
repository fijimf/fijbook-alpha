package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.Game

import scala.concurrent.Future

trait FeatureExtractor {
  def apply(gs: List[Game]): Future[List[(Long,Map[String, Double])]]
}

object FeatureExtractor {

  import scala.concurrent.ExecutionContext.Implicits.global

  def repeat(f: FeatureExtractor, n: Int): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[(Long,Map[String, Double])]] = {
        f(gs).map(fm => List.fill(n)(fm).flatten)
      }
    }
  }

  def stitch(fs: FeatureExtractor*): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[(Long,Map[String, Double])]] = {
        Future.sequence(fs.map(f => f(gs))).map(fms => fms.flatten.toList)
      }
    }
  }

  def mergeAll(fs: FeatureExtractor*): FeatureExtractor = {
    fs.toList.reduce(merge)
  }

  def merge(a: FeatureExtractor, b: FeatureExtractor): FeatureExtractor = {
    new FeatureExtractor {
      override def apply(gs: List[Game]): Future[List[(Long,Map[String, Double])]] = {
        for {
          x <- a(gs)
          y <- b(gs)
        } yield {
          x.zip(y).map(tup =>{
            require(tup._1._1==tup._2._1)
            (tup._1._1, tup._1._2 ++ tup._2._2)
          })
        }
      }
    }
  }
}





