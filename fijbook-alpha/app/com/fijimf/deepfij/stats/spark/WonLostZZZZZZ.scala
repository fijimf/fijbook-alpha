package com.fijimf.deepfij.stats.spark

import java.time.format.DateTimeFormatter

import com.fijimf.deepfij.models.Season
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.services.ScheduleSerializer.MappedUniverse
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

object WonLostZZZZZZ extends Serializable {
  val yyyymmdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)
    ScheduleSerializer.readLatestSnapshot().foreach(u => {
      wins(sc, u)
    })
  }

  def wins(sc: SparkContext, u: MappedUniverse): Map[Int, Map[String, Map[String, Int]]] = {

    u.seasons.map(s => {
      val dates = Season.dates(s.year).map(_.format(yyyymmdd))
      val fg: List[ScheduleSerializer.MappedGame] = s.scoreboards.flatMap(_.games)
      val games = sc.parallelize(fg)
      val ws: RDD[(String, String)] = games.flatMap(g => {
        val d = g.date.format(yyyymmdd)
        (g.result.get("homeScore"), g.result.get("awayScore")) match {
          case (Some(h), Some(a)) if h > a => Some(d -> g.homeTeamKey)
          case (Some(h), Some(a)) if h < a => Some(d -> g.awayTeamKey)
          case _ => None
        }
      })
      val winMap = dates.take(25).map(d => {
        d -> ws.filter(_._1 <= d).map(_._2).countByValue().mapValues(_.toInt).toMap
      }).toMap
      s.year -> winMap
    }).toMap
  }
}


  
  