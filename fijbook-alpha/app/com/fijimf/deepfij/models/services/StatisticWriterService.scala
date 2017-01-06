package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.Schedule
import com.fijimf.deepfij.stats.{Model, Stat}

import scala.concurrent.Future
import scala.language.postfixOps


trait StatisticWriterService {

  val models:List[Model[_]]

  def update(): Option[Future[Option[Int]]]
  def update(date:LocalDate): Option[Future[Option[Int]]]

  def updateForSchedule(sch: Schedule): Future[Option[Int]]

  def lookupModel(modelKey:String):Option[Model[_]]
  def lookupStat(modelKey:String,statKey:String):Option[Stat[_]]
}