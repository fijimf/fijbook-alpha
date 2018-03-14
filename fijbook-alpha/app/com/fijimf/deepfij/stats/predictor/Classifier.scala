package com.fijimf.deepfij.stats.predictor

trait Classifier[T] {

  def categories:Int

  def featureMapper: FeatureMapper[T]

  def classify(t: T): Option[List[Double]]

  def betaCoefficients: List[(String, Double)]
}
