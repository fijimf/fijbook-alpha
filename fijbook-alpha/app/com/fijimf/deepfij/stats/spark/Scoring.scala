package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.services.elasticmapreduce.model.StepConfig
import org.apache.spark.sql._
import org.apache.spark.sql.functions._


object Scoring extends Serializable with SparkStepConfig with DeepFijStats with StatsDbAccess {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def createStatistics(session: SparkSession, timestamp: String): DataFrame = {

    val games = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")
    val teams = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/teams.parquet")

    calculate(session, games, teams)
  }

   def calculate(session: SparkSession, games: DataFrame,teams:DataFrame) = {
    import session.implicits._
    val results = games.filter("(home_score is not null) and (away_score is not null)")

    val s = $"season".as("game_season")
    val d = $"date".as("game_date")

    val ht = $"home_team".as("team")
    val at = $"away_team".as("team")
    val hs = $"home_score".as("score")
    val as = $"away_score".as("score")

    val hmrg = ($"home_score" - $"away_score").as("score")
    val amrg = ($"away_score" - $"home_score").as("score")
    val comb =($"home_score" + $"away_score").as("score")

    val pointsFor = results.select(s, d, ht, hs).union(results.select(s, d, at, as))
    val pointsAgainst = results.select(s, d, ht, as).union(results.select(s, d, at, hs))
    val margin = results.select(s, d, ht, hmrg).union(results.select(s, d, at, amrg))
    val combined = results.select(s, d, ht,comb ).union(results.select(s, d, at,comb))

    val seasonDates = loadDates(session, games).coalesce(1)

    val data = Map("points_for" -> pointsFor, "points_against" -> pointsAgainst, "points_margin" -> margin, "points_combined" -> combined)
    val value = data.map { case (k, v) => calcStats(session, k, v, seasonDates, teams) }
    value.reduceLeft(_.union(_))
  }

  def calcStats(session: SparkSession, k: String, v: DataFrame, dates: DataFrame, teams:DataFrame): DataFrame = {
    import session.implicits._
    val list: DataFrame = dates.join(v,
      dates.col("season") === v.col("game_season") &&
        dates.col("date") > v.col("game_date")
    )

    val seasonDateTeams = dates.crossJoin(teams).select($"season", $"date", $"key".as("team")).coalesce(1)

    val z: DataFrame = seasonDateTeams.join(list.groupBy($"season", $"date", $"team")
      .agg(
        mean($"score").as("_mean"),
        stddev($"score").as("_std_dev"),
        min($"score").as("_min"),
        max($"score").as("_max")
      ),List("season","date","team"),"left")
    val meanVal = z.select($"season", $"date", $"team", $"_mean".as("value")).withColumn("stat", lit(s"${k}_mean"))
    val stddevVal = z.select($"season", $"date", $"team", $"_std_dev".as("value")).withColumn("stat", lit(s"${k}_std_dev"))
    val minVal = z.select($"season", $"date", $"team", $"_min".as("value")).withColumn("stat", lit(s"${k}_min"))
    val maxVal = z.select($"season", $"date", $"team", $"_max".as("value")).withColumn("stat", lit(s"${k}_max"))
    meanVal.union(stddevVal).union(minVal).union(maxVal)
  }

  override def stepConfig(extraOptions: Map[String, String]): StepConfig = createStepConfig(
    "Calculate scoring stats",
    "com.fijimf.deepfij.stats.spark.Scoring",
    extraOptions
  )

  override def appName: String = "Scoring"
}


  
  