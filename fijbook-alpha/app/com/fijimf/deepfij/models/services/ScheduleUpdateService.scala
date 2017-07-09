package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Season}
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {
  def updateSeason(optDates:Option[List[LocalDate]], mailReport:Boolean)
  def updateSeason(optDates:Option[List[LocalDate]], s:Season, mailReport:Boolean)
  def updateDb(keys:List[String],updateData: List[GameMapping]):Future[Iterable[(Seq[Long], Seq[Long])]]
}