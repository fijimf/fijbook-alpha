package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain}
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.stats.spark.WonLost.createWonLostStatistics
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.scalatest.{DoNotDiscover, FunSpec}


@DoNotDiscover
class ScoringSpec extends FunSpec {

  describe("The Scoring calculator") {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Jim Rules")
      .set("spark.ui.enabled", "true")
    val spark: SparkSession = SparkSession.builder()
      .config(
        conf
      ).getOrCreate()
    val credentials: AWSCredentials = new DefaultAWSCredentialsProviderChain().getCredentials

    spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", credentials.getAWSAccessKeyId)
    spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", credentials.getAWSSecretKey)
    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")

    val games = spark.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")

    val s = Scoring.createScoringStatistics(spark, games)
    
    List("mean", "std_dev", "min","max").foreach(u=>{
      List("points_for", "points_against", "points_margin","points_combined").foreach(t=>{
        it(s"should calculate ${t}_${u}") {
          val rows = testRows(s, s"${t}_${u}")
          assert(rows.count() > 0)
          rows.show(60)
        }
      })
    })
 
 
  }

  private def testRows(wl: Dataset[Row], stat: String) = {
    wl.where(s"season=2018 and team='georgetown' and stat='$stat'").orderBy("date")
  }
}
