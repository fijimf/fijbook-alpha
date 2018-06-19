package com.fijimf.deepfij.stats.spark

import java.io.InputStream

import com.fijimf.deepfij.models.services.ScheduleSerializer.MappedUniverse
import com.fijimf.deepfij.models.services.ScheduleSerializerSpec
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{DoNotDiscover, FunSpec}
import play.api.libs.json.Json

import scala.io.Source

@DoNotDiscover
class MarginRegressionGenerationSpec extends FunSpec {

  describe("The MarginRegression model") {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("DEEPFIJ")
      .set("spark.ui.enabled", "true")
    val spark: SparkSession = SparkSession.builder()
      .config(
        conf
      ).getOrCreate()
    val isS3Json: InputStream = classOf[ScheduleSerializerSpec].getResourceAsStream("/test-data/s3Sched.json")
    val s3Sched: Array[Byte] = Source.fromInputStream(isS3Json).mkString.getBytes

    val universe = Json.parse(s3Sched).as[MappedUniverse]

    val teams = TeamsDataFrame.create(spark, universe)
    val games = GamesDataFrame.create(spark, universe)
    val (m, n, latency) = {
      val start = System.currentTimeMillis()
      val mrg = MarginRegression.calculate(spark, games, teams)
      val end = System.currentTimeMillis()
      (mrg, mrg.count(), end - start)
    }

    it("should generate results in less than 30 seconds") {
      assert(n > 0)
      assert(latency < 30000)
    }

    it("should generate won, lost & wp for each team for each date") {
      assert(m.filter("season = 2018 and date ='2017-11-11 00:00:00' and stat='base_ols'").count == 351)
    }

   //TODO actually test values calculated
  }
}
