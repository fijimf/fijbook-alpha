package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.services.elasticmapreduce.model.StepConfig
import org.apache.spark.sql._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.functions._


object WonLost extends Serializable with SparkStepConfig with DeepFijStats with StatsDbAccess {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  val winner: (String, Int, String, Int) => Option[String] = (h, hs, a, as) => if (hs > as) Some(h) else if (as > hs) Some(a) else None
  val winnerUdf: UserDefinedFunction = udf(winner)
  val loser: (String, Int, String, Int) => Option[String] = (h, hs, a, as) => if (hs < as) Some(h) else if (as < hs) Some(a) else None
  val loserUdf: UserDefinedFunction = udf(loser)
  val wp: (Int, Int) => Double = (w, l) => if ((w + l) > 0) w.toDouble / (w + l).toDouble else 0.0

  val wpUdf: UserDefinedFunction = udf(wp)

  def createStatistics(session: SparkSession, timestamp: String): DataFrame = {
    import session.implicits._

    val games = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")
    val results =
      games.filter("(home_score is not null) and (away_score is not null) and (away_score<>home_score)")
        .withColumn("winner", winnerUdf($"home_team", $"home_score", $"away_team", $"away_score"))
        .withColumn("loser", loserUdf($"home_team", $"home_score", $"away_team", $"away_score"))
        .select($"season".as("game_season"), $"date".as("game_date"), $"winner", $"loser")

    val seasonDates = loadDates(session, games)
    val accumulatedResults = seasonDates.join(
      results,
      seasonDates.col("season") === results.col("game_season") &&
        seasonDates.col("date") > results.col("game_date")
    )
    val wins = accumulatedResults
      .groupBy($"season", $"date", $"winner")
      .agg(count("winner").as("won"))
      .select($"season", $"date", $"winner".as("team"), $"won".as("value")).withColumn("stat", lit("won"))

    val losses = accumulatedResults
      .groupBy($"season", $"date", $"loser")
      .agg(count("loser").as("lost"))
      .select($"season", $"date", $"loser".as("team"), $"lost".as("value")).withColumn("stat", lit("lost"))

    val wp = wins.select($"season", $"date", $"team", $"value".as("won"))
      .join(
        losses.select($"season", $"date", $"team", $"value".as("lost")),
        List("season", "date", "team"),
        "outer")
      .select($"season", $"date", $"team", $"won", $"lost")
      .na.fill(0, List("won", "lost"))
      .withColumn("value", wpUdf($"won", $"lost"))
      .select($"season", $"date", $"team", $"value").withColumn("stat", lit("wp"))

    wins.union(losses).union(wp)
  }

  override def stepConfig(extraOptions: Map[String, String]): StepConfig = createStepConfig(
    "Calculate won/lost stats",
    "com.fijimf.deepfij.stats.spark.WonLost",
    extraOptions
  )

  override def appName: String = "WonLost"
}


  
  