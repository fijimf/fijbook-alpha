package com.fijimf.deepfij.stats.spark

import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

object ConferencesDataFrame {
  def create(session: SparkSession, u: ScheduleSerializer.MappedUniverse): DataFrame = {
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

}
