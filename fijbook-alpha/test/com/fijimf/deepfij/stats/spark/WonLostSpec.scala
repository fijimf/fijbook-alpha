package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain}
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{DoNotDiscover, FunSpec}


@DoNotDiscover
class WonLostSpec extends FunSpec {

  describe("The WonLostCalculator") {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Jim Rules")
      .set("spark.ui.enabled", "true")
    val spark: SparkSession = SparkSession.builder()
      .config(
        conf
      ).getOrCreate()
    val credentials: AWSCredentials = new DefaultAWSCredentialsProviderChain().getCredentials
    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")
    it("should not fail") {
      WonLost.run(spark, timestamp, Some(credentials))
    }
//    val wl =  spark.read.csv(s"s3n://deepfij-emr/data/stats/$timestamp/wonlost.csv")
//    it("should create 'won'") {
//      val won = wl.filter("team='georgetown' and season = 2018 and stat='won'")
//      assert(won.collect().length>0)
//      won.sort(wl.col("date")).show(50)
//    }
//   it("should create 'lost'") {
//     val lost = wl.filter("team='georgetown' and season = 2018 and stat='lost'")
//     lost.sort(wl.col("date")).show(50)
//     assert(lost.collect().length>0)
//    }
//   it("should create 'wp'") {
//     val wp = wl.filter("team='georgetown' and season = 2018 and stat='wp'")
//     wp.sort(wl.col("date")).show(50)
//     assert(wp.collect().length>0)
//    }

  }
}
