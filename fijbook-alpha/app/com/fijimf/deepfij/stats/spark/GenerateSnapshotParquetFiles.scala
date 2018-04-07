package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

object GenerateSnapshotParquetFiles extends Serializable {
  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Word Count")
    val session = SparkSession.builder().config(conf).getOrCreate()
    run(session)
  }

  def run(session: SparkSession): Option[String] = {

    val credentials = new DefaultAWSCredentialsProviderChain().getCredentials
    session.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", credentials.getAWSAccessKeyId)
    session.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", credentials.getAWSSecretKey)

    ScheduleSerializer.readLatestSnapshot().map(u => {
      val stampKey = u.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      val ds = ScheduleSerializer.deleteObjects("deepfij-emr", s"data/snapshots/$stampKey")
      createTeamDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/teams.parquet")
      createConferenceDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferences.parquet")
      createConferenceMapDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferenceMaps.parquet")
      stampKey
    })
  }

  private def createTeamDf(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val teamData = u.teams.map(t => Row(t.key, t.name, t.nickname))
    val structType = StructType(List(
      StructField("key", StringType, nullable = false),
      StructField("name", StringType, nullable = false),
      StructField("nickname", StringType, nullable = false)
    ))
    session.createDataFrame(
      session.sparkContext.parallelize(teamData),
      structType
    )
  }

  private def createConferenceDf(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val conferenceData = u.conferences.map(t => Row(t.key, t.name))
    val structType = StructType(List(
      StructField("key", StringType, nullable = false),
      StructField("name", StringType, nullable = false)
    ))
    session.createDataFrame(
      session.sparkContext.parallelize(conferenceData),
      structType
    )
  }

  private def createConferenceMapDf(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val confMapData =u.seasons.flatMap(s=>{
      s.confMap.flatMap(cm=>{
        cm.teams.map(t=>Row(s.year,cm.key,t))
      })
    })
    val structType = StructType(List(
      StructField("season", IntegerType, nullable = false),
      StructField("conference_key", StringType, nullable = false),
      StructField("team_key", StringType, nullable = false)
    ))
    session.createDataFrame(
      session.sparkContext.parallelize(confMapData),
      structType
    )
  }
  
}


  
  