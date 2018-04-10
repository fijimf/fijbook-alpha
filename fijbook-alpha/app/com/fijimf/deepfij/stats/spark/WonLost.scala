package com.fijimf.deepfij.stats.spark

import java.sql.Timestamp
import java.time.format.DateTimeFormatter

import com.amazonaws.auth.AWSCredentials
import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.services.ScheduleSerializer
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.sql.expressions._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


object WonLost extends Serializable {

  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  val winner: (String, Int, String, Int) => Option[String] = (h, hs, a, as) => if (hs > as) Some(h) else if (as > hs) Some(a) else None
  val winnerUdf: UserDefinedFunction = udf(winner)

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Create parquet schedule files")
    val session = SparkSession.builder().config(conf).getOrCreate()
    val timestamp = ScheduleSerializer.readLatestSnapshot().map(_.timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).getOrElse("")
    run(session,timestamp)
  }

  def run(session: SparkSession, timestamp: String, overrideCredentials:Option[AWSCredentials]=None): DataFrame = {
    import session.implicits._
    
    overrideCredentials.foreach(cred=>{
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", cred.getAWSAccessKeyId)
      session.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", cred.getAWSSecretKey)
    })
    
    val games = session.read.parquet(s"s3n://deepfij-emr/data/snapshots/$timestamp/games.parquet")
    
    val seasons = games.select(col("season")).distinct()
//    val dates = seasons.flatMap(r=>Season.dates(r.getAs[Integer]("season")).map(d=>Row(r.getAs[Integer]("season"),Timestamp.valueOf(d.atStartOfDay()))))
    val results = games.filter("(home_score is not null) and (away_score is not null) and (away_score<>home_score)")
    results.withColumn("winner", winnerUdf(results.col("home_team"),results.col("home_score"),results.col("away_team"),results.col("away_score")))

    val seasonDates = loadDates(session, games)
    seasonDates
  }

  private def loadDates(session: SparkSession, games: DataFrame) = {
    import session.implicits._

    val rows = session.sparkContext.parallelize(games.select($"season".as[Int]).distinct().collect().toList.flatMap(y => {
      Season.dates(y).map(d => {
        Row(y, Timestamp.valueOf(d.atStartOfDay()))
      })
    }))
    session.createDataFrame(rows, StructType(List(
      StructField("season", IntegerType, nullable = false),
      StructField("date", TimestampType, nullable = false)
    )
    ))
  }
}


  
  