package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.stats.spark.WonLost.dbProperties
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, TimestampType}

trait DeepFijStats {

  def main(args: Array[String]): Unit = {
    val start =  Timestamp.valueOf(LocalDateTime.now())
    val conf = new SparkConf().setAppName(appName)
    val session = SparkSession.builder().config(conf).getOrCreate()
    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")
    enrichedSanitizedStats(createStatistics(session, timestamp))
      .write
      .mode("append")
      .jdbc("jdbc:mysql://www.fijimf.com:3306/deepfijdb", "_xstat", dbProperties())
    val end =  Timestamp.valueOf(LocalDateTime.now())
    import session.implicits._
    Seq((appName, timestamp, start, end)).toDF("name", "timestamp", "start", "finish")
      .write
      .mode("append")
      .jdbc("jdbc:mysql://www.fijimf.com:3306/deepfijdb", "_xstat_metadata", dbProperties())
  }

  def enrichedSanitizedStats(frame:DataFrame): DataFrame = {
    enrichTeamStats(
      frame.na.drop(Array("value"))
    ).na.replace(
      Seq("mean", "stddev"),
      Map("NaN" -> "null")
    )
  }

  def appName: String

  def createStatistics(session: SparkSession, timestamp: String): DataFrame

  def enrichTeamStats(stats: DataFrame): DataFrame = {
    val summary = stats
      .groupBy("season", "date", "stat")
      .agg(
        mean("value").as("mean"),
        stddev("value").as("stddev"),
        min("value").as("min"),
        max("value").as("max"),
        count("value").as("n")
      )
    val up = Window.partitionBy("season", "date", "stat").orderBy(asc("value"))
    val dn = Window.partitionBy("season", "date", "stat").orderBy(desc("value"))
    stats
      .withColumn("rank_asc", rank().over(up))
      .withColumn("rank_desc", rank().over(dn))
      .withColumn("pctile_asc", cume_dist().over(up))
      .withColumn("pctile_desc", cume_dist().over(dn))
      .join(summary, Seq("season", "date", "stat"))
  }

  def loadDates(session: SparkSession, games: DataFrame): DataFrame = {
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
}


  
  