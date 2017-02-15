package com.fijimf.deepfij.stats.predictor

import org.apache.mahout.classifier.sgd.{L1, OnlineLogisticRegression}
import org.apache.mahout.math.{Matrix, Vector}
import play.api.Logger


trait FeatureMapper[T] {
  def featureDimension: Int

  def featureName(i: Int): String

  def feature(t: T): Option[Vector]
}

trait Categorizer[T] {
  def numCategories: Int

  def categorize(t: T): Option[Int]
}

case class LogisticReg[T](fm: FeatureMapper[T], cat: Categorizer[T]) {
  val logger = Logger(this.getClass)
  val logisticRegression: OnlineLogisticRegression = new OnlineLogisticRegression(cat.numCategories, fm.featureDimension, new L1()).lambda(0.0).learningRate(1)


  def regress(data: List[T], numPasses: Int = 25): List[Double] = {
    val trainingSet = for (d <- data; feat <- fm.feature(d); c <- cat.categorize(d)) yield feat -> c
    logger.info(s"Training set size ${trainingSet.size}")
    1.to(numPasses).foreach(i => {
      logger.info(s"Pass #$i")
      trainingSet.foreach(obs => {
        logisticRegression.train(obs._2, obs._1)
      })
      logger.info(logisticRegression.getBeta.toString)
    })
    val beta: Matrix = logisticRegression.getBeta
    0.until(fm.featureDimension).map { i =>
      val b: Double = beta.get(0, i)
      logger.info(f" ${fm.featureName(i)}%-20s  $b%7.4f")
      b
    }.toList
  }

  def classify(v:Vector) = {
    logisticRegression.classifyFull(v).get(1)
  }
}