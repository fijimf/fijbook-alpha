package com.fijimf.deepfij.stats.predictor

final case class LogisticPerformanceSplit[B](key: B, correct: Int = 0, incorrect: Int = 0, avgLogLikelihood: Double = 0.0) {
  require(correct >= 0 && incorrect >= 0)

  def n: Int = correct + incorrect

  def pctCorrect: Double = correct.toDouble / n.toDouble

  def combine(newKey: B, x: LogisticPerformanceSplit[B]): LogisticPerformanceSplit[B] = {
    LogisticPerformanceSplit(newKey, correct + x.correct, incorrect + x.incorrect, (n * avgLogLikelihood + x.n * x.avgLogLikelihood) / (n + x.n))
  }
}
