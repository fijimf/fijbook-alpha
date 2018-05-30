package com.fijimf.deepfij.stats.spark

import java.io.InputStream

import com.fijimf.deepfij.models.services.ScheduleSerializer.MappedUniverse
import com.fijimf.deepfij.models.services.ScheduleSerializerSpec
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.FunSpec
import play.api.libs.json.Json

import scala.io.Source

class DataFrameCreationSpec extends FunSpec {

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
     val frame = TeamsDataFrame.create(spark,universe)
      assert(frame.count()===351)
      frame.show()
    }
    it("should create a conferences DataFrame") {
     val frame = ConferencesDataFrame.create(spark,universe)
      assert(frame.count()===32)
      frame.show()
    }
    it("should create a conferences map DataFrame") {
     val frame =ConferencesMapDataFrame.create(spark,universe)
      assert(frame.count()===5 * 351)
      frame.show()
    }
    it("should create a games DataFrame") {
     val frame = GamesDataFrame.create(spark,universe)
      assert(frame.count()===27753)
      frame.show()
    }
  }
}
