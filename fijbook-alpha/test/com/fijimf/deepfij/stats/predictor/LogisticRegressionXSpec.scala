package com.fijimf.deepfij.stats.predictor

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.{Game, Result}
import com.fijimf.deepfij.stats.predictor.sparkml.LogisticRegressionX
import org.apache.spark.SparkConf
import org.apache.spark.ml.linalg.{DenseVector, Vector}
import org.apache.spark.sql.SparkSession
import org.scalatest.FlatSpec

import scala.util.Random

class LogisticRegressionXSpec extends FlatSpec {

  def fixture(name: String) = new {
    val spark: SparkSession = SparkSession.builder().config(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName(name)
        .set("spark.ui.enabled", "false")
    ).getOrCreate()

    val fm: FeatureMapper[(Game, Option[Result])] = new FeatureMapper[(Game, Option[Result])] {
      override def featureName(i: Int): String = List("x", "y", "z")(i)

      override def feature(t: (Game, Option[Result])): Option[Vector] = Option(new DenseVector(Array(1.0, math.pow(t._1.homeTeamId,1.5)-math.pow(t._1.awayTeamId,1.5), t._1.homeTeamId - t._1.awayTeamId)))

      override def featureDimension: Int = 3
    }

    def getTestData(numTeams: Int, numDays: Int, gamesPerDay: Int, numDaysResults: Int, seed: Long = 0L): List[(Game, Option[Result])] = {
      require(numTeams % 2 == 0)
      val rng = new Random(seed)
      val today = LocalDate.now()
      1.to(numDays).toList.flatMap(d => {
        val tuples: Traversable[(Long, Long)] = rng.shuffle(1L.to(numTeams).toList).splitAt(numTeams / 2).zipped.take(gamesPerDay)
        val date = today.plusDays(d)
        if (d >= numDaysResults) {
          tuples.map { case (h, a) => (
            Game(0L, 1L, h, a, date, date.atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
            None
          )
          }
        } else {
          tuples.map { case (h, a) => (
            Game(0L, 1L, h, a, date, date.atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
            Some(Result(0L, 0L, 50  + 2 * h.toInt, 50 + 2 * a.toInt, 2, LocalDateTime.now(), "Test"))
          )
          }
        }
      })
    }


    val obs = List(
      (
        Game(1L, 1L, 1L, 2L, LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 1).atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
        Some(
          Result(1L, 1L, 99, 78, 2, LocalDateTime.now, "test")
        )
      ), (
        Game(2L, 1L, 2L, 3L, LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 1).atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
        Some(
          Result(2L, 2L, 99, 78, 2, LocalDateTime.now, "test")
        )
      ), (
        Game(3L, 1L, 3L, 1L, LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 1).atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
        Some(
          Result(3L, 3L, 99, 78, 2, LocalDateTime.now, "test")
        )
      ), (
        Game(4L, 1L, 1L, 2L, LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 1).atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
        None
      ), (
        Game(5L, 1L, 1L, 2L, LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 1).atStartOfDay(), None, false, None, None, None, "Test", LocalDateTime.now(), "Test"),
        None
      )
    )
  }

  "A LogisticRegressionX" should "be created for the provided configuration" in {
    val appName = new Random().nextString(12)
    println(s"Name $appName")
    val f = fixture(appName)
    val lrx = LogisticRegressionX(f.spark)
    assert(lrx.spark.sparkContext.appName == appName)
  }


  it should "be able to create classifier" in {
    val appName = new Random().nextString(12)
    println(s"Name $appName")
    val f = fixture(appName)

    val lrx = LogisticRegressionX(f.spark)
    lrx.createClassifier(f.fm, StraightWinCategorizer, f.getTestData(32,24,12,20,73L))
  }

}
