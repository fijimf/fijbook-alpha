package com.fijimf.deepfij.stats.predictor

import org.apache.spark.ml.linalg.Vector

trait FeatureMapper[T] {
  def featureDimension: Int

  def featureName(i: Int): String

  def feature(t: T): Option[Vector]

  def featureMap(t: T): Option[Map[String, Double]] = {
    feature(t).map(v => {
      0.until(v.size).map(n => featureName(n) -> v(n)).toMap
    })
  }

}
