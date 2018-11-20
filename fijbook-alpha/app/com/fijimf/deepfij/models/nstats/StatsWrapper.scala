package com.fijimf.deepfij.models.nstats

import java.time.LocalDate
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.pattern._
import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.nstats.actor.SnapshotBuffer
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{CalcStatus, Schedule, XStat}
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

final case class StatsWrapper(dao: ScheduleDAO, actorSystem: ActorSystem) {
  val logger = Logger(this.getClass)

  private def writeSnapshot(a: Analysis[_], s: Schedule, key: String, snapshot: Snapshot): Unit = {
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

      actorSystem.actorSelection(s"/user/$key") ! SnapshotDbBundle(snapshot.date, a.key, xstats)
    }
  }

  def updateStats(s: Schedule, models: List[Analysis[_]], timeout:FiniteDuration): Future[Any]= {

    import scala.concurrent.ExecutionContext.Implicits.global
    val instanceId = UUID.randomUUID().toString
    val key = s"snapshot-$instanceId"
    val buffer = actorSystem.actorOf(Props(classOf[SnapshotBuffer], dao), key)

    val schedHash = ScheduleSerializer.md5Hash(s)

    logger.info(s"Hash value for ${s.season.year} is $schedHash")
    Future.sequence(models.map(m => {
      dao.findStatus(s.season.id, m.key).map {
        case Some(c) if c.hash === schedHash =>
          logger.info(s"Skipping model ${m.key} for ${s.season.year}.  Stats are up to date")
          Option.empty[Analysis[_]]
        case _ => Some(m)
      }
    })).map(_.flatten).map {
      case Nil =>
        logger.info("**** No models to run")
        sendCompletionMessage(timeout, key, buffer)
      case m :: Nil =>
        for {
          _ <- dao.deleteXStatBySeason(s.season, m.key)
          _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m.key, schedHash))
        } yield {
          Analysis.analyzeSchedule(s, m, writeSnapshot(m, s, key, _), terminateCallback(timeout, key, buffer))
        }
      case m :: ms =>
        ms.foreach(m1 => {
          for {
            _ <- dao.deleteXStatBySeason(s.season, m1.key)
            _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m1.key, schedHash))
          } yield {
            Analysis.analyzeSchedule(s, m1, writeSnapshot(m1, s, key, _))
          }
        })
        for {
          _ <- dao.deleteXStatBySeason(s.season, m.key)
          _ <- dao.saveStatus(CalcStatus(0L, s.season.id, m.key, schedHash))
        } yield {
          Analysis.analyzeSchedule(s, m, writeSnapshot(m, s, key, _), terminateCallback(timeout, key, buffer))
        }

    }
  }

  private def sendCompletionMessage(timeout: FiniteDuration, key: String, buffer: ActorRef) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val completionMessage = buffer.ask(SendingComplete)(timeout)
    completionMessage.onComplete { msg =>
      logger.info(s"Processing complete for $key.  Sending kill to write buffer")
      logger.info(s"$msg")
      logger.info(s" Sending kill to write buffer")
      buffer ! PoisonPill
    }
    completionMessage
  }

  private def terminateCallback(timeout: FiniteDuration, key: String, buffer: ActorRef): ((GameCalendar, _)) => Boolean = {
    tup => {
      val done = tup._1.date.isEmpty
      if (done) {
        sendCompletionMessage(timeout, key, buffer)
      }
      done
    }
  }

}
