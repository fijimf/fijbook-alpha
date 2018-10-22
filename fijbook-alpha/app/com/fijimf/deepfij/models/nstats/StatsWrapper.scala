package com.fijimf.deepfij.models.nstats

import java.time.LocalDate

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.pattern._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Schedule, XStat}
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration._


final case class Snapshot(date: LocalDate, obs: Map[Long, Double]) {
  val n = obs.size
  val (mean, stdDev, min, max) = if (obs.nonEmpty) {
    val s = new DescriptiveStatistics(obs.values.toArray)
    //final case class XStat(id:Long, seasonId:Long, date: LocalDate, key: String, teamId: Long, value: Option[Double], rankAsc: Option[Int], rankDesc: Option[Int], percentileAsc: Option[Double], percentileDesc: Option[Double], mean: Option[Double], stdDev: Option[Double], min: Option[Double], max: Option[Double], n: Int)
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

  def value(id: Long): Option[Double] = obs.get(id)

  def rank(id: Long): Option[Int] = rankMap.get(id)

  def percentile(id: Long): Option[Double] = rankMap.get(id).map(_.toDouble / n)

}

final case class SnapshotDbBundle(d: LocalDate, k: String, xs: List[XStat])

class StatsWrapper(dao: ScheduleDAO, actorSystem: ActorSystem) {
  val logger = Logger(this.getClass)

  def writeSnapshot(a:Analysis[_], s:Schedule, snapshot: Snapshot): Unit = {
    if (snapshot.n > 0) {
      val xstats = s.teams.map(t => {
        val id = t.id
        XStat(
          id = 0L,
          seasonId = s.season.id,
          date = snapshot.date,
          key = a.key,
          teamId = id,
          value = snapshot.value(id),
          rank = snapshot.rank(id),
          percentile = snapshot.percentile(id),
          mean = snapshot.mean,
          stdDev = snapshot.stdDev,
          min = snapshot.min,
          max = snapshot.max,
          n = snapshot.n
        )
      })

      actorSystem.actorSelection("/user/snapshot-buffer") ! SnapshotDbBundle(snapshot.date, a.key, xstats)
    }
  }

  def updateStats(s: Schedule, models: List[Analysis[_]], timeout:FiniteDuration): Future[Any]= {

    val buffer = actorSystem.actorOf(Props(classOf[SnapshotBuffer], dao), "snapshot-buffer")

    models.foreach(m => {
      Analysis.analyzeSchedule(s, m, writeSnapshot(m, s, _))
    })

    buffer.ask(CalculationComplete)(timeout)

  }





}
