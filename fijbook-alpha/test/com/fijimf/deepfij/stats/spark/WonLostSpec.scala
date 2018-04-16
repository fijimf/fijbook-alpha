package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain}
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.scalatest.{DoNotDiscover, FunSpec}


@DoNotDiscover
class WonLostSpec extends FunSpec {

  describe("The WonLost calculator") {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("DEEPFIJ")
      .set("spark.ui.enabled", "true")
    val spark: SparkSession = SparkSession.builder()
      .config(
        conf
      ).getOrCreate()
    val credentials: AWSCredentials = new DefaultAWSCredentialsProviderChain().getCredentials
    spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", credentials.getAWSAccessKeyId)
    spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", credentials.getAWSSecretKey)

    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")

    val wl = WonLost.createStatistics(spark, timestamp)
    it("should calculate won") {
      val rows = testRows(wl, "won")
      assert(rows.count() > 0)
      rows.show(60)

    }
    it("should calculate lost") {
      val rows = testRows(wl, "lost")
      assert(rows.count() > 0)
      rows.show(60)

    }
    it("should calculate wp") {
      val rows = testRows(wl, "wp")
      assert(rows.count() > 0)
      rows.show(60)
    }
  }

  private def testRows(wl: Dataset[Row], stat: String) = {
    wl.where(s"season=2018 and team='georgetown' and stat='$stat'").orderBy("date")
  }
}
