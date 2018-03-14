package com.fijimf.deepfij.stats.predictor

import org.apache.mahout.classifier.sgd.{L1, OnlineLogisticRegression, PriorFunction}
import play.api.Logger

import scala.util.Random



object LogisticReg {
  val logger = Logger(this.getClass)

  def createClassifier[T](fm: FeatureMapper[T], cat: Categorizer[T], observations: List[T], prior: PriorFunction = new L1(), lambda: Double = 0.0, learningRate: Double = 1.0, numPasses: Int = 25): Classifier[T] = {
    try {
      logger.info("Creating OnlineLogisticRegression")
      val olr = null // new OnlineLogisticRegression(cat.numCategories, fm.featureDimension, prior).lambda(lambda).learningRate(learningRate)
//
//      val trainingSet = for (d <- observations; feat <- fm.feature(d); c <- cat.categorize(d)) yield feat -> c
//      logger.info("Creating training set")
//      logger.info(s"Training set size ${trainingSet.size}")
//      1.to(numPasses).foreach(i => {
//        logger.info(s"Pass #$i")
//        Random.shuffle(trainingSet).foreach(obs => {
//          olr.train(obs._2, obs._1)
//        })
//        println(olr.getBeta)
//      })

    new Classifier[T] {
      override def categories: Int = 2

      override def featureMapper: FeatureMapper[T] = fm

      override def classify(t: T): Option[List[Double]] = {
        featureMapper.feature(t) match {
//          case Some(v)=>
//            val cv = olr.classifyFull(v)
//            Some(0.until(categories).map(cv.get).toList)
          case None=>
            logger.warn(s"When classifying failed to map features on $t")
            None
        }
      }

      override def betaCoefficients: List[(String, Double)] = {
        0.until(fm.featureDimension).map { i =>
//          val beta = olr.getBeta
//          val b: Double = beta.get(0, i)
//          fm.featureName(i) -> b
      ""->0.0
        }.toList
      }
    }
    } catch {
      case thr:Throwable=> logger.error("",thr)
        throw new RuntimeException(thr)
    }

  }


}