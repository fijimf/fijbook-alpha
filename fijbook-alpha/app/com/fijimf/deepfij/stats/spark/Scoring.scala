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


object Scoring extends Serializable with SparkStepConfig with StatsDbAccess {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

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

    createScoringStatistics(session, games).write.mode("append").jdbc("jdbc:mysql://www.fijimf.com:3306/deepfijdb", "xstats", dbProperties())
  }

  def createScoringStatistics(session: SparkSession, games: DataFrame): DataFrame = {
    import session.implicits._
    val results = games.filter("(home_score is not null) and (away_score is not null)")
    val pointsFor = results.select($"season".as("game_season"), $"date".as("game_date"), $"home_team".as("team"), $"home_score".as("score"))
      .union(results.select($"season".as("game_season"), $"date".as("game_date"), $"away_team".as("team"), $"away_score".as("score")))
    val pointsAgainst = results.select($"season".as("game_season"), $"date".as("game_date"), $"home_team".as("team"), $"away_score".as("score"))
      .union(results.select($"season".as("game_season"), $"date".as("game_date"), $"away_team".as("team"), $"home_score".as("score")))
    val margin = results.select($"season".as("game_season"), $"date".as("game_date"), $"home_team".as("team"), ($"home_score"-$"away_score").as("score"))
      .union(results.select($"season".as("game_season"), $"date".as("game_date"), $"away_team".as("team"), ($"away_score"-$"home_score").as("score")))
    val combined = results.select($"season".as("game_season"), $"date".as("game_date"), $"home_team".as("team"), ($"home_score"+$"away_score").as("score"))
      .union(results.select($"season".as("game_season"), $"date".as("game_date"), $"away_team".as("team"), ($"away_score"+$"home_score").as("score")))
       

    val seasonDates = loadDates(session, games)

    val data: Map[String, DataFrame] = Map("points_for" -> pointsFor, "points_against" -> pointsAgainst, "points_margin" -> margin, "points_combined" -> combined)
    val value= data.map {case (k,v)=>calcStats(session, k,v, seasonDates)}
    value.reduceLeft(_.union(_))
  }
  
  def calcStats(session: SparkSession, k:String, v:DataFrame, dates:DataFrame):DataFrame={
    import session.implicits._
    val list:DataFrame = dates.join(v,
      dates.col("season") === v.col("game_season") &&
        dates.col("date") > v.col("game_date")
    )

    val z:DataFrame = list.groupBy($"season", $"date", $"team")
      .agg(
        mean($"score").as("_mean"),
        stddev($"score").as("_std_dev"),
        min($"score").as("_min"),
        max($"score").as("_max")
      )
    val meanVal = z.select($"season", $"date", $"team", $"_mean".as("value")).withColumn("stat", lit(s"${k}_mean"))
    val stddevVal = z.select($"season", $"date", $"team", $"_std_dev".as("value")).withColumn("stat", lit(s"${k}_std_dev"))
    val minVal = z.select($"season", $"date", $"team", $"_min".as("value")).withColumn("stat", lit(s"${k}_min"))
    val maxVal = z.select($"season", $"date", $"team", $"_max".as("value")).withColumn("stat", lit(s"${k}_max"))
    meanVal.union(stddevVal).union(minVal).union(maxVal)
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
    "Calculate scoring stats",
    "com.fijimf.deepfij.stats.spark.Scoring",
    extraOptions
  )

}


  
  