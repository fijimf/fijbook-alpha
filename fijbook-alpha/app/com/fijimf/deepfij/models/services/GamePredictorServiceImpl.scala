package com.fijimf.deepfij.models.services

import javax.inject.Inject

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.stats.predictor.{LinearRegressionPredictor, LogisticRegressionPredictor, SchedulePredictor}
import play.api.Logger

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.language.postfixOps

class GamePredictorServiceImpl @Inject()(dao: ScheduleDAO) extends GamePredictorService {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(this.getClass)

  val activeYear = 2017

  val models: List[ScheduleDAO=>SchedulePredictor] = List(
    (scheduleDao:ScheduleDAO)=>LinearRegressionPredictor(scheduleDao),
    (scheduleDao:ScheduleDAO)=> LogisticRegressionPredictor("wp", scheduleDao),
    (scheduleDao:ScheduleDAO)=> LogisticRegressionPredictor("x-margin-ties", scheduleDao),
    (scheduleDao:ScheduleDAO)=> LogisticRegressionPredictor("rpi121", scheduleDao)
  )

  override def update() = {
    logger.info("Updating game predictions for the latest schedule")
    val result: Option[Schedule] = Await.result(dao.loadSchedules().map(_.find(_.season.year == activeYear)), Duration.Inf)
    result.map(sch => {
      logger.info(s"Got schedule ${sch.season.year}")
      Future.sequence(models.map(_(dao).predictSchedule(sch)))
    })

  }
}