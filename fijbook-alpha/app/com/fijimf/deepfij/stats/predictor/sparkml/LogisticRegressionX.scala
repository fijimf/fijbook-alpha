package com.fijimf.deepfij.stats.predictor.sparkml

import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.{Game, Result}
import com.fijimf.deepfij.stats.predictor.{Categorizer, FeatureMapper}
import org.apache.spark.SparkConf
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.linalg
import org.apache.spark.sql.SparkSession
import play.api.Logger

object LogisticRegressionXXXX {

  val logger = Logger(this.getClass)

  def create(): LogisticRegressionX = LogisticRegressionX(
    SparkSession.builder()
      .config(
        new SparkConf()
          .setMaster("local[*]")
          .setAppName("Jim Rules")
          .set("spark.ui.enabled", "false")
      ).getOrCreate()
  )


}

final case class LogisticRegressionX(spark: SparkSession) {
  val logger = Logger(this.getClass)


  def createClassifier(fm: FeatureMapper[(Game, Option[Result])], cat: Categorizer[(Game, Option[Result])], observations: List[(Game, Option[Result])]) = {

    import spark.implicits._



    val values: List[(Long, String, Long, Long, Option[Long], Option[Int], Option[Int], Option[Int], Option[linalg.Vector], Option[Double])] = for {
      obs <- observations
    } yield {
      (
        obs._1.id,
        obs._1.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
        obs._1.homeTeamId,
        obs._1.awayTeamId,
        obs._2.map(_.id),
        obs._2.map(_.homeScore),
        obs._2.map(_.awayScore),
        obs._2.map(_.periods),
        fm.feature(obs),
        cat.categorize(obs).map(_.toDouble)
      )
    }
       val training = values.filter(_._10.isDefined).toDF("gameId","gameDate","homeTeamId","awayTeamId","resultId","homeScore","awayScore","periods", "features", "label" )

        val lr = new LogisticRegression()
          .setMaxIter(50)
          .setRegParam(0.3)
          .setElasticNetParam(0.8)

    println(lr)
        // Fit the model
        val lrModel = lr.fit(training)
println(lrModel)
   print(lrModel.summary.objectiveHistory.mkString("\n"))
  //  training.show()
    val regressionSummary = lrModel.evaluate(values.toDF("gameId","gameDate","homeTeamId","awayTeamId","resultId","homeScore","awayScore","periods", "features", "label" ))

    //regressionSummary.predictions.show(100)
    println(lrModel.coefficients)
    println(lrModel.intercept)
  }


}