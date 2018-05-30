package com.fijimf.deepfij.stats.spark

import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

object TeamsDataFrame {
  def create(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
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
}
