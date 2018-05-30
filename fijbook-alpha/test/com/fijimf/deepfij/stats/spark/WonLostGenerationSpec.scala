package com.fijimf.deepfij.stats.spark

import java.io.InputStream

import com.fijimf.deepfij.models.services.ScheduleSerializer.MappedUniverse
import com.fijimf.deepfij.models.services.ScheduleSerializerSpec
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.FunSpec
import play.api.libs.json.Json

import scala.io.Source

class WonLostGenerationSpec extends FunSpec {

  describe("The dataframe creators") {
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

    it("should create a teams DataFrame") {
      import spark.implicits._
      val teams = TeamsDataFrame.create(spark, universe)
      val conferences = ConferencesDataFrame.create(spark, universe)
      val conferencesMap = ConferencesMapDataFrame.create(spark, universe)
      val games = GamesDataFrame.create(spark, universe)

      val wl: DataFrame = WonLost.calculate(spark, games)

      assert(wl.filter("season = 2018 and date ='2017-11-11 00:00:00' and stat='won'").count==351)

    }
  }
}