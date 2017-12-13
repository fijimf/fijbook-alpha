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

class ComputedStatisticServiceImpl @Inject()(dao: ScheduleDAO) extends ComputedStatisticService {
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
    models.foldLeft(
      Future.successful(0)
    )(
      (fi: Future[Int], model: Model[_]) => {
        logger.info(s"Updating ${model.name} for ${sch.season.year} for ${dates.size} dates.")
        val ds = model.canCreateDates(sch,dates)
        model.create(sch, ds) match {
          case Some(analyzer) =>
            updateDates(sch, analyzer, ds).flatMap(fj => fi.map(_ + fj))
          case None =>
            fi
        }
      }
    )
  }

  def getValues(sch:Schedule, dates:List[LocalDate], model:Analyzer[_]): List[StatValue] = {
    model.model.stats.flatMap(getValues(sch, dates, model, _))
  }

  def getValues(sch:Schedule, dates:List[LocalDate], model:Analyzer[_], stat:Stat[_]): List[StatValue] = {
    val sk = stat.key
    val mk = model.model.key
    val defaultValue = stat.defaultValue
    for {
      d <- dates
      t <- sch.teams
    } yield {
      model.value(sk, t, d) match {
        case Some(x) if x.isInfinity=>StatValue(0L, mk, sk, t.id, d, defaultValue)
        case Some(x) if x.isNaN=>StatValue(0L, mk, sk, t.id, d, defaultValue)
        case Some(x)=>StatValue(0L, mk, sk, t.id, d, defaultValue)
        case None=>StatValue(0L, mk, sk, t.id, d, defaultValue)
      }
    }
  }

  def updateDates(sch: Schedule, model: Analyzer[_], dates: List[LocalDate]): Future[Int] = {
    val statValues = (for {
      s <- model.model.stats
      d <- dates
      t <- sch.teams
    } yield {
      model.value(s.key, t, d).map(x =>
        if (x.isInfinity || x.isNaN) {
          StatValue(0L, model.model.key, s.key, t.id, d, s.defaultValue)
        } else {
          StatValue(0L, model.model.key, s.key, t.id, d, x)
        })
    }).flatten
    dao.saveStatValues(dates, List(model.model.key), statValues).map(_.size)
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

