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

  describe("The WonLost model") {
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
    val (wl,n, latency) = {
      val start = System.currentTimeMillis()
      val wl = WonLost.calculate(spark, games, teams)
      val end = System.currentTimeMillis()
      (wl, wl.count(), end - start)
    }

    it ("should generate results in less than 30 seconds"){
      assert(n>0)
      assert(latency<30000)
    }

    it("should generate won, lost & wp for each team for each date") {
      assert(wl.filter("season = 2018 and date ='2017-11-11 00:00:00' and stat='won'").count == 351)
      assert(wl.filter("season = 2018 and date ='2017-11-11 00:00:00' and stat='lost'").count == 351)
      assert(wl.filter("season = 2018 and date ='2017-11-11 00:00:00' and stat='wp'").count == 351)
      assert(wl.filter("season = 2017 and date ='2016-12-31 00:00:00' and stat='won'").count == 351)
      assert(wl.filter("season = 2017 and date ='2016-12-31 00:00:00' and stat='lost'").count == 351)
      assert(wl.filter("season = 2017 and date ='2016-12-31 00:00:00' and stat='wp'").count == 351)
    }

    it ("should generate correct win totals for a team") {
      def x(dateStr:String, statStr:String) = wl.filter(s"season = 2018 and date = '$dateStr' and stat='$statStr' and team='georgetown'").select("value")
      assert(x("2017-11-12 00:00:00", "won").head.getAs[Double](0)===0.0)
      assert(x("2017-11-13 00:00:00", "won").head.getAs[Double](0)===1.0)
      assert(x("2017-12-13 00:00:00", "won").head.getAs[Double](0)===8.0)
      assert(x("2017-12-16 00:00:00", "won").head.getAs[Double](0)===8.0)
      assert(x("2017-12-17 00:00:00", "won").head.getAs[Double](0)===8.0)
      assert(x("2018-03-23 00:00:00", "won").head.getAs[Double](0)===15.0)
    }

    it ("should generate correct loss totals for a team") {
      def x(dateStr:String, statStr:String) = wl.filter(s"season = 2018 and date = '$dateStr' and stat='$statStr' and team='georgetown'").select("value")
      assert(x("2017-11-12 00:00:00", "lost").head.getAs[Double](0)===0.0)
      assert(x("2017-11-13 00:00:00", "lost").head.getAs[Double](0)===0.0)
      assert(x("2017-12-13 00:00:00", "lost").head.getAs[Double](0)===0.0)
      assert(x("2017-12-16 00:00:00", "lost").head.getAs[Double](0)===0.0)
      assert(x("2017-12-17 00:00:00", "lost").head.getAs[Double](0)===1.0)
      assert(x("2018-03-23 00:00:00", "lost").head.getAs[Double](0)===15.0)
    }

    it ("should generate correct wp totals for a team") {
      def x(dateStr:String, statStr:String) = wl.filter(s"season = 2018 and date = '$dateStr' and stat='$statStr' and team='georgetown'").select("value")
      assert(x("2017-11-12 00:00:00", "wp").head.getAs[Double](0)===0.0)
      assert(x("2017-11-13 00:00:00", "wp").head.getAs[Double](0)===1.0)
      assert(x("2017-12-13 00:00:00", "wp").head.getAs[Double](0)===1.0)
      assert(x("2017-12-16 00:00:00", "wp").head.getAs[Double](0)===1.0)
      assert(x("2017-12-17 00:00:00", "wp").head.getAs[Double](0)===8.0/9.0)
      assert(x("2018-03-23 00:00:00", "wp").head.getAs[Double](0)===0.5)
    }
  }
}