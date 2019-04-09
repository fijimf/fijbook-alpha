package com.fijimf.deepfij.models.nstats

import java.time.LocalDate
import cats.implicits._

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

final case class Snapshot(date: LocalDate, obs: Map[Long, Double]) {
  val n: Int = obs.size
  val (mean, stdDev, min, max) = if (obs.nonEmpty) {
    val s = new DescriptiveStatistics(obs.values.toArray)
    //final case class XStat(id:Long, seasonId:Long, date: LocalDate, key: String, teamId: Long, value: Option[Double], rankAsc: Option[Int], rankDesc: Option[Int], percentileAsc: Option[Double], percentileDesc: Option[Double], mean: Option[Double], stdDev: Option[Double], min: Option[Double], max: Option[Double], n: Int)
    (Some(s.getMean), Some(s.getStandardDeviation), Some(s.getMin), Some(s.getMax))
  } else {
    (None, None, None, None)
  }

  val rankMap: Map[Long, Int] = obs.toList.sortBy(_._2).foldLeft(List.empty[(Long, Double, Int, Int)]) { case (list, (key, value)) =>
    if (list.isEmpty) {
      List((key, value, 1, 1))
    } else {
      val x = list.head
      if (value === x._2) {
        (key, value, x._3, x._4 + 1) :: list
      } else {
        (key, value, x._4 + 1, x._4 + 1) :: list
      }
    }
  }.map(t => t._1 -> t._3).toMap

  def value(id: Long): Option[Double] = obs.get(id)

  def rank(id: Long): Option[Int] = rankMap.get(id)

  def percentile(id: Long): Option[Double] = rankMap.get(id).map(_.toDouble / n)

  def isEmpty(): Boolean = obs.isEmpty

  def nonEmpty(): Boolean = obs.nonEmpty

}
