package com.fijimf.deepfij.stats.spark

import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.FunSpec

class GenerateSnapshotParquetFilesSpec extends FunSpec {

  describe("The parquet file generator") {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Jim Rules")
      .set("spark.ui.enabled", "true")
    val spark: SparkSession = SparkSession.builder()
      .config(
        conf
      ).getOrCreate()
    val optStamp = GenerateSnapshotParquetFiles.run(spark)

    it("should not fail") {
      if (optStamp.isEmpty) fail()
    }
    val jsonSnap = ScheduleSerializer.readLatestSnapshot()

    it("Should generate a correct teams parquet file") {
      for {
        timestamp <- optStamp
        mappedUniverse <- jsonSnap
      } {
        val teams: DataFrame = spark.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/teams.parquet")
        assert(teams.count == mappedUniverse.teams.size)
        teams.show(25)
        teams.filter("nickname = 'Hoyas'").show()
      }
    }
    it("Should generate a correct conferences parquet file") {
      for {
        timestamp <- optStamp
        mappedUniverse <- jsonSnap
      } {
        val conferences: DataFrame = spark.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/conferences.parquet")
        assert(conferences.count == mappedUniverse.conferences.size)
        conferences.show(25)
        conferences.filter("key = 'big-east'").show()
      }
    }
    it("Should generate a correct conferenceMaps parquet file") {
      for {
        timestamp <- optStamp
        mappedUniverse <- jsonSnap
      } {
        val conferences: DataFrame = spark.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/conferenceMaps.parquet")
        assert(conferences.count == mappedUniverse.seasons.map(_.confMap.map(_.teams.size).sum).sum)
        conferences.show(25)
        conferences.filter("conference_key = 'big-east'").show()
      }
    }
  }
}
