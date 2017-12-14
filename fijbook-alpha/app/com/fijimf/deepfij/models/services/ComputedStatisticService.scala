package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.stats.{Model, Stat}

import scala.concurrent.Future
import scala.language.postfixOps


trait ComputedStatisticService {
  val models: List[Model[_]]

  def update(lastNDays: Option[Int] = None): Option[Future[Int]]

  def updateAllSchedules(key:Option[String]): Future[List[Int]]

  def lookupModel(modelKey: String): Option[Model[_]]

  def lookupStat(modelKey: String, statKey: String): Option[Stat[_]]
}