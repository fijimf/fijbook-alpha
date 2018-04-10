package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain}
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
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
    
    val wl = WonLost.run(spark, timestamp, Some(credentials))
    
    

    it("Shouldn't fail") {
wl.show()
    }
   
  }
}
