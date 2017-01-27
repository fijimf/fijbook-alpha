package com.fijimf.deepfij.models.services

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.stats._
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

class StatisticWriterServiceImpl @Inject()(dao: ScheduleDAO) extends StatisticWriterService {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val activeYear = 2017

  val models: List[Model[_]] = List(WonLost, Scoring, Rpi, LeastSquares)

  val statMap: Map[String, Map[String, Stat[_]]] = models.map(m => m.key -> m.stats.map(s => s.key -> s).toMap).toMap
  val modelMap: Map[String, Model[_]] = models.map(m => m.key -> m).toMap

  override def update(lastNDays: Option[Int]): Option[List[Unit]] = {
    logger.info("Updating calculated statistics for the latest schedule")
    val result: Option[Schedule] = Await.result(dao.loadSchedules().map(_.find(_.season.year == activeYear)), Duration.Inf)
    result.map(sch => {
      logger.info(s"Found ${sch.season.year} as the current schedule")
      val dates = lastNDays match {
        case Some(n) => scheduleValidDates(sch).takeRight(n)
        case None => scheduleValidDates(sch)
      }
      if (dates.nonEmpty) {
        logger.info(s"Models will be run and saved for the ${dates.size} between ${dates.head} and ${dates.last}, inclusive")
        updateForSchedule(sch, dates)
      } else {
        List.empty[Unit]
      }
    })
  }

  def updateForSchedule(sch: Schedule, dates: List[LocalDate]): List[Unit] = {
    val batchSize = 15
    (for (ds <- dates.grouped(batchSize);
          m <- List(WonLost(sch, ds), Scoring(sch, ds), Rpi(sch, ds), LeastSquares(sch, ds))
    ) yield {
      val values: List[StatValue] = (for (s <- m.stats;
                                          d <- ds;
                                          t <- sch.teams) yield {
        m.value(s.key, t, d).map(x =>
          if (x.isInfinity) {
            logger.warn(s"For $d, ${s.key}, ${t.name} value is Infinity.  Setting to default value ${s.defaultValue}")
            StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
          } else if (x.isNaN) {
            logger.warn(s"For $d, ${s.key}, ${t.name} value is NaN.  Setting to default value ${s.defaultValue}")
            StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
          } else {
            StatValue(0L, m.key, s.key, t.id, d, x)
          }
        )
      }).flatten
      dao.saveStatValues(batchSize, ds, List(m.key), values)
    }).toList
  }

  private def scheduleValidDates(sch: Schedule) = {
    val startDate = sch.resultDates.minBy(_.toEpochDay)
    val endDate = sch.resultDates.maxBy(_.toEpochDay)
    Iterator.iterate(startDate) {
      _.plusDays(1)
    }.takeWhile(!_.isAfter(endDate)).toList
  }


  override def lookupStat(modelKey: String, statKey: String): Option[Stat[_]] = {
    statMap.get(modelKey).flatMap(m => m.get(statKey))
  }

  override def lookupModel(modelKey: String): Option[Model[_]] = {
    modelMap.get(modelKey)
  }
}