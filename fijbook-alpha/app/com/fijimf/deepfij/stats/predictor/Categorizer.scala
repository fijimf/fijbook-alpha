package com.fijimf.deepfij.stats.predictor

trait Categorizer[T] {
  def numCategories: Int

  def categorize(t: T): Option[Int]
}
