package com.fijimf.deepfij.models.services

import java.time.LocalDate
import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.stats._
import play.api.Logger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class StatisticWriterServiceImpl @Inject()(dao: ScheduleDAO) extends StatisticWriterService {
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val models: List[Model[_]] = List(WonLost, Scoring, Rpi, LeastSquares)

  val statMap: Map[String, Map[String, Stat[_]]] = models.map(m => m.key -> m.stats.map(s => s.key -> s).toMap).toMap
  val modelMap: Map[String, Model[_]] = models.map(m => m.key -> m).toMap

  override def update(lastNDays: Option[Int]): Option[Future[Int]] = {
    logger.info("Updating calculated statistics for the latest schedule")
    val result: Option[Schedule] = Await.result(dao.loadLatestSchedule(), Duration.Inf)
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
        Future.successful(0)
      }
    })
  }

  override def updateAllSchedules(): Future[List[Int]] = {
    dao.loadSchedules().flatMap(ss=>{
      logger.info(s"Loaded schedules for ${ss.size} years")
      Future.sequence(ss.map(s=>{ val dates = scheduleValidDates(s)
        logger.info(s"Loading stats for ${s.season.year}")
        if (dates.nonEmpty) {
          logger.info(s"Models will be run and saved for the ${dates.size} between ${dates.head} and ${dates.last}, inclusive")
          updateForSchedule(s, dates)
        } else {
          Future.successful(0)
        }}))
    })
  }

  def updateForSchedule(sch: Schedule, dates: List[LocalDate]): Future[Int] = {
    val models = List(WonLost(sch, dates), Scoring(sch, dates), Rpi(sch, dates), LeastSquares(sch, dates))
    models.foldLeft(
      Future.successful(0)
    )(
      (futInt: Future[Int], model: Analyzer[_]) => futInt.flatMap(i => updateDates(sch, model, dates).map(_ + i))
    )
  }

  def updateDates(sch: Schedule, model: Analyzer[_], dates: List[LocalDate]): Future[Int] = {
    dao.saveStatValues(dates, List(model.key), (for {
      s <- model.stats
      d <- dates
      t <- sch.teams
    } yield {
      model.value(s.key, t, d).map(x =>
        if (x.isInfinity || x.isNaN) {
          StatValue(0L, model.key, s.key, t.id, d, s.defaultValue)
        } else {
          StatValue(0L, model.key, s.key, t.id, d, x)
        })
    }).flatten).map(_.size)
  }


  def scheduleValidDates(sch: Schedule): List[LocalDate] = {
    if (sch.resultDates.isEmpty){
      logger.warn(s"No valid dates for ${sch.season.year}")
      List.empty[LocalDate]
    } else {
      val startDate = sch.resultDates.minBy(_.toEpochDay)
      val endDate = sch.resultDates.maxBy(_.toEpochDay)
      Iterator.iterate(startDate) {
        _.plusDays(1)
      }.takeWhile(!_.isAfter(endDate)).toList
    }
  }


  override def lookupStat(modelKey: String, statKey: String): Option[Stat[_]] = {
    statMap.get(modelKey).flatMap(m => m.get(statKey))
  }

  override def lookupModel(modelKey: String): Option[Model[_]] = {
    modelMap.get(modelKey)
  }
}

