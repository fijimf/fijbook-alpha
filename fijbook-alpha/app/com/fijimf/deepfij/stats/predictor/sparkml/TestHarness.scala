package com.fijimf.deepfij.stats.predictor.sparkml

import java.time.LocalDate
import java.time.temporal.ChronoUnit._
import java.util.Properties

import org.apache.spark.SparkConf
import org.apache.spark.sql.expressions.{UserDefinedFunction, Window}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object TestHarness {

  val spark: SparkSession = {
    SparkSession.builder()
      .config(
        new SparkConf()
          .setMaster("local[*]")
          .setAppName("Jim Rules")
          .set("spark.ui.enabled", "false")
      ).getOrCreate()
  }

  val daysBetween: UserDefinedFunction = udf[Long, String, String]((c, d) => {
    DAYS.between(LocalDate.parse(c), LocalDate.parse(d))
  })

  def main(args: Array[String]): Unit = {

    println("Hello world")
    val jdbcUrl = s"jdbc:mysql://localhost:3306/deepfijdb"
    val connectionProperties = new Properties()
    connectionProperties.put("user", "root")
    connectionProperties.put("password", "")

    val g = spark.read.jdbc(jdbcUrl, "game", connectionProperties).select("season_id","id", "date", "home_team_id", "away_team_id")
    val r = spark.read.jdbc(jdbcUrl, "result", connectionProperties)
    val s = spark.read.jdbc(jdbcUrl, "stat_value", connectionProperties)
//    val t = spark.read.jdbc(jdbcUrl, "stat_value", connectionProperties)

//    val g1 = g.withColumnRenamed("date", "game_date")
//    val j: DataFrame = joinStatsToGames(g1, s, "won-lost", "wstreak", "home")
//    val k: DataFrame = joinStatsToGames(j, s, "won-lost", "wstreak", "away")


    val k: DataFrame = List(
      ("won-lost", "wins"),
      ("won-lost", "losses"),
      ("won-lost", "wstreak"),
      ("won-lost", "lstreak"),
      ("won-lost", "wp")
    ).foldLeft(g.withColumnRenamed("date", "game_date"))((g1: DataFrame, tuple: (String, String)) => addStatToGames(g1, s, tuple._1, tuple._2))
    val kr = k.join(r,k("id")===r("game_id"),"left")
    val xx= System.nanoTime()
    kr.show()
    println((System.nanoTime()-xx)/1000000000L)
    println("Goodbye world")
    spark.close()

  }

  def addStatToGames(g: DataFrame, s: DataFrame, model: String, stat: String): DataFrame = {
    joinStatsToGames(joinStatsToGames(g, s, model, stat, "home"), s, model, stat, "away")
  }

  def joinStatsToGames(g: DataFrame, s: DataFrame, model: String, stat: String, ha: String): DataFrame = {
    val s1: DataFrame = createStatJoin(s, model, stat, ha)
    g.join(s1, g.col(s"${ha}_team_id") === s1.col("team_id") && g.col("game_date") === s1.col("next_date"), "inner")
      .drop("next_date", "date", "team_id")

  }

  def createStatJoin(statsTable: DataFrame, model: String, stat: String, label: String): DataFrame = {
    val w = Window.orderBy("date").partitionBy("team_id")
    val valueColumn = s"$model:$stat:$label"
    statsTable
      .filter(s"model_key = '$model' AND stat_key = '$stat'")
      .withColumn("next_date", lead("date", 1, "2999-01-01").over(w))
      //      .withColumn("gap", daysBetween($"next_date", $"date"))
      //      .filter("gap < 30")
      .withColumnRenamed("value", valueColumn)
      .select("next_date", "date", "team_id", valueColumn)

  }
}
