package com.fijimf.deepfij.models.services

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.stats._
import play.api.Logger

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class StatisticWriterServiceImpl @Inject()(dao: ScheduleDAO) extends StatisticWriterService {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val activeYear = 2017

  val models: List[Model[_]] = List(WonLost, Scoring, Rpi, LeastSquares)

  val statMap: Map[String, Map[String, Stat[_]]] = models.map(m => m.key -> m.stats.map(s => s.key -> s).toMap).toMap
  val modelMap: Map[String, Model[_]] = models.map(m => m.key -> m).toMap

  def update(): Option[Future[Unit]] = {
    val result: Option[Schedule] = Await.result(dao.loadSchedules().map(_.find(_.season.year == activeYear)), Duration.Inf)
    result.map(sch => {
      updateForSchedule(sch)
    })
  }

  def update(date: LocalDate): Option[Future[Unit]] = {
    val result: Option[Schedule] = Await.result(dao.loadSchedules().map(_.find(_.season.year == activeYear)), Duration.Inf)
    result.map(sch => {
      val dates = (-2).to(3).map(i => date.plusDays(i)).toList
      updateDatesForSchedule(sch, dates, List(WonLost(sch), Scoring(sch), Rpi(sch), LeastSquares(sch, dates)))
    })
  }

  def updateForSchedule(sch: Schedule): Future[Unit] = {
    //    val models: List[Analyzer[_]] = List( LeastSquares(sch))
    val dates = sch.lastResult match {
      case Some(d) => sch.season.dates.filter(sd => sd.isBefore(d))
      case None => List.empty
    }
    val models: List[Analyzer[_]] = List(WonLost(sch), Scoring(sch), Rpi(sch), LeastSquares(sch, dates))

    updateDatesForSchedule(sch, dates, models)
  }

  private def updateDatesForSchedule(sch: Schedule, dates: List[LocalDate], models: List[Analyzer[_]]): Future[Unit] = {
    logger.info("Updating stats for dates " + dates.take(5).mkString(", ") + (if (dates.size > 5) "..." else ""))

    val values: List[StatValue] = (for (m <- models;
                                        s <- m.stats;
                                        d <- dates;
                                        t <- sch.teams
    ) yield {
      m.value(s.key, t, d).map(x =>
        if (x.isInfinity) {
          logger.warn("For " + d + ", " + s.key + ", " + t.name + " value is Infinity.  Setting to default value " + s.defaultValue)
          StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
        } else if (x.isNaN) {
          logger.warn("For " + d + ", " + s.key + ", " + t.name + " value is NaN.  Setting to default value " + s.defaultValue)
          StatValue(0L, m.key, s.key, t.id, d, s.defaultValue)
        } else {
          StatValue(0L, m.key, s.key, t.id, d, x)
        }
      )
    }).flatten
    dao.saveStatValues(dates, models.map(_.key), values)
  }

  override def lookupStat(modelKey: String, statKey: String): Option[Stat[_]] = {
    statMap.get(modelKey).flatMap(m => m.get(statKey))
  }

  override def lookupModel(modelKey: String): Option[Model[_]] = {
    modelMap.get(modelKey)
  }
}