package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.elasticmapreduce.model.StepConfig
import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


object WonLost extends Serializable with SparkStepConfig with StatsDbAccess {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  val winner: (String, Int, String, Int) => Option[String] = (h, hs, a, as) => if (hs > as) Some(h) else if (as > hs) Some(a) else None
  val winnerUdf: UserDefinedFunction = udf(winner)
  val loser: (String, Int, String, Int) => Option[String] = (h, hs, a, as) => if (hs < as) Some(h) else if (as < hs) Some(a) else None
  val loserUdf: UserDefinedFunction = udf(loser)
  val wp: (Int, Int) => Double = (w, l) => if ((w + l) > 0) w.toDouble / (w + l).toDouble else 0.0

  val wpUdf: UserDefinedFunction = udf(wp)

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Create won lost statistics")
    val session = SparkSession.builder().config(conf).getOrCreate()
    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")
    run(session, timestamp)
  }

  def run(session: SparkSession, timestamp: String, overrideCredentials: Option[AWSCredentials] = None): Unit = {

    overrideCredentials.foreach(cred => {
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", cred.getAWSAccessKeyId)
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", cred.getAWSSecretKey)
    })

    val games = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")
    val teams = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/teams.parquet")

    createWonLostStatistics(session, games).write.mode("append").jdbc("jdbc:mysql://www.fijimf.com:3306/deepfijdb", "xstats", dbProperties())
  }

  def createWonLostStatistics(session: SparkSession, games: DataFrame): Dataset[Row] = {
    import session.implicits._
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

    val wp = wins.withColumn("won", $"value")
      .join(
        losses.withColumn("lost", $"value"),
        List("season", "date", "team"),
        "outer")
      .select($"season", $"date", $"team", $"won", $"lost")
      .na.fill(0, List("won", "lost"))
      .withColumn("wp", wpUdf($"won", $"lost"))
      .select($"season", $"date", $"team", $"wp".as("value")).withColumn("stat", lit("wp"))
    wins.union(losses).union(wp)
  }

  private def loadDates(session: SparkSession, games: DataFrame): DataFrame = {
    import session.implicits._
    games.select($"season".as[Int]).distinct().flatMap(y => {
      Season.dates(y).map(d => {
        Row(y, Timestamp.valueOf(d.atStartOfDay()))
      })
    })(
      RowEncoder(
        StructType(
          List(
            StructField("season", IntegerType, nullable = false),
            StructField("date", TimestampType, nullable = false)
          )
        )
      )
    )
  }

  override def stepConfig(extraOptions: Map[String, String]): StepConfig = createStepConfig(
    "Calculate won/lost stats",
    "com.fijimf.deepfij.stats.spark.WonLost",
    extraOptions
  )

}


  
  