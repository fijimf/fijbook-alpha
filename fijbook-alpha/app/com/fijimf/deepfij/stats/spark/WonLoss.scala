package com.fijimf.deepfij.stats.spark

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.services.ScheduleSerializer.MappedUniverse
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object WonLoss extends SparkStats with Serializable {
  val yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd")

  def wins(u:MappedUniverse):Map[Int,Map[String,Map[String,Int]]] = {
    u.seasons.map(s=>{
      val dates = Season.dates(s.year).map(_.format(yyyymmdd))
      val fg: List[ScheduleSerializer.MappedGame] = s.scoreboards.flatMap(_.games)
      val games =spark.sparkContext.parallelize(fg)
      val ws: RDD[(String, String)] = games.flatMap(g => {
        val d = g.date.format(yyyymmdd)
        (g.result.get("homeScore"), g.result.get("awayScore")) match {
          case (Some(h), Some(a)) if h > a => Some(d -> g.homeTeamKey)
          case (Some(h), Some(a)) if h < a => Some(d -> g.awayTeamKey)
          case _ => None
        }
      })
      val winMap= dates.take(25).map(d => {
        d -> ws.filter(_._1 <= d).map(_._2).countByValue().mapValues(_.toInt).toMap
      }).toMap
      s.year->winMap
    }).toMap
  }
}


trait SparkStats {
  val spark:SparkSession= SparkSession.builder()
    .config(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("Jim Rules")
        .set("spark.ui.enabled", "true")
    ).getOrCreate()
}


  
  