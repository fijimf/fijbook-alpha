package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.elasticmapreduce.model.StepConfig
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.types._

object GenerateSnapshotParquetFiles extends Serializable with SparkStepConfig {
  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Create parquet schedule files")
    val session = SparkSession.builder().config(conf).getOrCreate()
    run(session)
  }

  def run(session: SparkSession, overrideCredentials: Option[AWSCredentials] = None): Option[String] = {

    overrideCredentials.foreach(cred => {
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", cred.getAWSAccessKeyId)
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", cred.getAWSSecretKey)
    })
    ScheduleSerializer.readLatestSnapshot().map(u => {
      val stampKey = u.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      val ds = ScheduleSerializer.deleteObjects("deepfij-emr", s"data/snapshots/$stampKey")
      TeamsDataFrame.create(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/teams.parquet")
      ConferencesDataFrame.create(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferences.parquet")
      ConferencesMapDataFrame.create(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferenceMaps.parquet")
      GamesDataFrame.create(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/games.parquet")
      stampKey
    })
  }

  override def stepConfig(extraOptions: Map[String, String]): StepConfig = createStepConfig(
    "Generate parquet schedule files",
    "com.fijimf.deepfij.stats.spark.GenerateSnapshotParquetFiles",
    extraOptions
  )

}


  
  