package com.fijimf.deepfij.stats.spark

import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

object ConferencesMapDataFrame {

  def create(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
    val confMapData = u.seasons.flatMap(s => {
      s.confMap.flatMap(cm => {
        cm.teams.map(t => Row(s.year, cm.key, t))
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
