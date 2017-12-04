package com.fijimf.deepfij.stats.predictor

import java.lang

import org.apache.mahout.classifier.sgd.{L1, OnlineLogisticRegression, PriorFunction}
import org.apache.mahout.math.Vector
import play.api.Logger

import scala.util.Random

trait FeatureMapper[T] {
  def featureDimension: Int

  def featureName(i: Int): String

  def feature(t: T): Option[Vector]

  def featureMap(t: T): Option[Map[String, Double]] = {
    feature(t).map(v => {
      0.until(v.size).map(n => featureName(n) -> v.get(n)).toMap
    })
  }

}

trait Categorizer[T] {
  def numCategories: Int

  def categorize(t: T): Option[Int]
}

trait Classifier[T] {

  def categories:Int

  def featureMapper: FeatureMapper[T]

  def classify(t: T): Option[List[Double]]

  def betaCoefficients: List[(String, Double)]
}

object LogisticReg {
  val logger = Logger(this.getClass)

  def createClassifier[T](fm: FeatureMapper[T], cat: Categorizer[T], observations: List[T], prior: PriorFunction = new L1(), lambda: Double = 0.0, learningRate: Double = 1.0, numPasses: Int = 25): Classifier[T] = {
    try {
      logger.info("Creating OnlineLogisticRegression")
      val olr = new OnlineLogisticRegression(cat.numCategories, fm.featureDimension, prior).lambda(lambda).learningRate(learningRate)

      val trainingSet = for (d <- observations; feat <- fm.feature(d); c <- cat.categorize(d)) yield feat -> c
      logger.info("Creating training set")
      logger.info(s"Training set size ${trainingSet.size}")
      1.to(numPasses).foreach(i => {
        logger.info(s"Pass #$i")
        Random.shuffle(trainingSet).foreach(obs => {
          olr.train(obs._2, obs._1)
        })
        println(olr.getBeta)
      })

    new Classifier[T] {
      override def categories: Int = 2

      override def featureMapper: FeatureMapper[T] = fm

      override def classify(t: T): Option[List[Double]] = {
        featureMapper.feature(t).map(v => {
          val cv = olr.classifyFull(v)
          0.until(categories).map(cv.get).toList
        })
      }

      override def betaCoefficients: List[(String, Double)] = {
        0.until(fm.featureDimension).map { i =>
          val beta = olr.getBeta
          val b: Double = beta.get(0, i)
          fm.featureName(i) -> b
        }.toList
      }
    }
    } catch {
      case thr:Throwable=> logger.error("",thr)
        throw new RuntimeException(thr)
    }

  }


}