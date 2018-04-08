package com.fijimf.deepfij.scraping.nextgen.tasks

import java.time.LocalDate

import akka.actor.ActorRef
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{Schedule, Season, StatValue}
import com.fijimf.deepfij.scraping.nextgen.{SSTask, SSTaskProgress}
import com.fijimf.deepfij.stats._
import play.api.Logger

import scala.concurrent.Future

case class UpdateStatistics(dao:ScheduleDAO, key:Option[String]) extends SSTask[Unit]{
  import scala.concurrent.ExecutionContext.Implicits.global
  val logger=Logger(this.getClass)

  val models: List[Model[_]] = List(WonLost, Scoring, Rpi, LeastSquares)
  val statMap: Map[String, Map[String, Stat[_]]] = models.map(m => m.key -> m.stats.map(s => s.key -> s).toMap).toMap
  val modelMap: Map[String, Model[_]] = models.map(m => m.key -> m).toMap

  override def safeToRun: Future[Boolean] = Future.successful(true)

  override def name: String = "Update statistics"

  override def run(messageListener: Option[ActorRef]): Future[Unit] = {
    updateAllSchedules(messageListener).map(_.size)
  }

  def updateAllSchedules(messageListener:Option[ActorRef]): Future[List[Int]] = {
    val ms = key match {
      case Some(k)=>models.filter(_.key==k)
      case None=>models
    }
    dao.loadSchedules().flatMap(ss=>{
      logger.info(s"Loaded schedules for ${ss.size} years")
      Future.sequence(ss.map(s=>{ val dates = scheduleValidDates(s)
        logger.info(s"Loading stats for ${s.season.year}")
        if (dates.nonEmpty) {
          logger.info(s"Models will be run and saved for the ${dates.size} between ${dates.head} and ${dates.last}, inclusive")
          updateForSchedule(s, dates, ms, s.season,messageListener)
        } else {
          Future.successful(0)
        }}))
    })
  }

  def updateForSchedule(sch: Schedule, dates: List[LocalDate], ms:List[Model[_]],season:Season, messageListener:Option[ActorRef]): Future[Int] = {
    ms.foldLeft(
      Future.successful(0)
    )(
      (fi: Future[Int], model: Model[_]) => {
        logger.info(s"Updating ${model.name} for ${sch.season.year} for ${dates.size} dates.")
        val ds = model.canCreateDates(sch,dates)
        model.create(sch, ds) match {
          case Some(analyzer) =>
            updateDates(sch, analyzer, ds, season, messageListener).flatMap(fj => fi.map(_ + fj))
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

  def updateDates(sch: Schedule, model: Analyzer[_], dates: List[LocalDate], season:Season, messageListener:Option[ActorRef]): Future[Int] = {
    val statValues = (for {
      s <- model.model.stats
      d <- dates
      t <- sch.teams
    } yield {
      messageListener.foreach(_ ! SSTaskProgress(None, Some(s"Calculating ${model.model.key} for ${season.year}")))
      model.value(s.key, t, d).map(x =>
        if (x.isInfinity || x.isNaN) {
          StatValue(0L, model.model.key, s.key, t.id, d, s.defaultValue)
        } else {
          StatValue(0L, model.model.key, s.key, t.id, d, x)
        })
    }).flatten
    messageListener.foreach(_ ! SSTaskProgress(None, Some(s"Saving ${model.model.key} for ${season.year}")))
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

}