package com.fijimf.deepfij.models.nstats

import java.time.LocalDate

import com.fijimf.deepfij.models.Schedule
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics


case class Snapshot(date: LocalDate, obs: Map[Long, Double]) {
  val n = obs.size
  val (mean, stdDev, min, max) = if (obs.nonEmpty) {
    val s = new DescriptiveStatistics(obs.values.toArray)
    //case class XStat(id:Long, seasonId:Long, date: LocalDate, key: String, teamId: Long, value: Option[Double], rankAsc: Option[Int], rankDesc: Option[Int], percentileAsc: Option[Double], percentileDesc: Option[Double], mean: Option[Double], stdDev: Option[Double], min: Option[Double], max: Option[Double], n: Int)
    (Some(s.getMean), Some(s.getStandardDeviation), Some(s.getMin), Some(s.getMax))
  } else {
    (None, None, None, None)
  }

  val rankMap: Map[Long, Int] = obs.toList.sortBy(_._2).foldLeft(List.empty[(Long, Double, Int, Int)]) { case (list, (key, value)) => {
    if (list.isEmpty) {
      List((key, value, 1, 1))
    } else {
      val x = list.head
      if (value == x._2) {
        (key, value, x._3, x._4 + 1) :: list
      } else {
        (key, value, x._4 + 1, x._4 + 1) :: list
      }
    }
  }
  }.map(t => t._1 -> t._3).toMap

}

class StatsWrapper(dao:ScheduleDAO) {

  def writeSnapshot(a:Analysis[_], s:Schedule, snapshot: Snapshot): Unit = {


  }

  def updateStats(data:Map[Long,Double], s:Schedule) = {

    for (a<-List(Regression.ols))
    Analysis.analyzeSchedule(s, a, writeSnapshot(a,s,_))

  }





}
