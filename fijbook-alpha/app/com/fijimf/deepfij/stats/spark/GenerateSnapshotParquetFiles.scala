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

  def run(session: SparkSession, overrideCredentials:Option[AWSCredentials]=None): Option[String] = {

    overrideCredentials.foreach(cred=>{
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", cred.getAWSAccessKeyId)
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", cred.getAWSSecretKey)
    })
    ScheduleSerializer.readLatestSnapshot().map(u => {
      val stampKey = u.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      val ds = ScheduleSerializer.deleteObjects("deepfij-emr", s"data/snapshots/$stampKey")
      createTeamDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/teams.parquet")
      createConferenceDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferences.parquet")
      createConferenceMapDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/conferenceMaps.parquet")
      createGameDf(session, u).write.parquet(s"s3n://deepfij-emr/data/snapshots/$stampKey/games.parquet")
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

  private def createGameDf(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val gameData =u.seasons.flatMap(s=>{
      s.scoreboards.flatMap(sb=>{
        sb.games.map(g=>
          Row(
            s.year,
            sb.date,
            Timestamp.valueOf(g.date.atStartOfDay()),
            Timestamp.valueOf(g.datetime),
            g.homeTeamKey,
            g.result.getOrElse(ScheduleSerializer.HOME_SCORE_KEY, null),
            g.awayTeamKey,
            g.result.getOrElse(ScheduleSerializer.AWAY_SCORE_KEY, null),
            g.result.getOrElse(ScheduleSerializer.PERIODS_KEY, null),
            g.isNeutralSite,
            g.tourneyKey,
            g.homeTeamSeed,
            g.awayTeamSeed,
            g.location
          )
        )
      })
    })
    val structType = StructType(List(
      StructField("season", IntegerType, nullable = false),
      StructField("logical_date", StringType, nullable = false),
      StructField("date", TimestampType, nullable = false),
      StructField("datetime", TimestampType, nullable = false),
      StructField("home_team", StringType, nullable = false),
      StructField("home_score", IntegerType, nullable = true),
      StructField("away_team", StringType, nullable = false),
      StructField("away_score", IntegerType, nullable = true),
      StructField("periods", IntegerType, nullable = true),
      StructField("neutral_site", BooleanType, nullable = false),
      StructField("tournament", StringType, nullable = true),
      StructField("home_team_seed", IntegerType, nullable = true),
      StructField("away_team_seed", IntegerType, nullable = true),
      StructField("location", StringType, nullable = true)
      
    ))
    session.createDataFrame(
      session.sparkContext.parallelize(gameData),
      structType
    )
  }

  override def stepConfig(extraOptions:Map[String, String]): StepConfig = createStepConfig(
    "Generate parquet schedule files",
    "com.fijimf.deepfij.stats.spark.GenerateSnapshotParquetFiles",
    extraOptions
  )
    
}


  
  