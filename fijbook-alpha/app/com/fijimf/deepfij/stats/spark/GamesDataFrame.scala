package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp

import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._

object GamesDataFrame {
  def create(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val gameData = u.seasons.flatMap(s => {
      s.scoreboards.flatMap(sb => {
        sb.games.map(g =>
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
}
