package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Season}
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {
  def update(optDates:Option[List[Int]], mailReport:Boolean)
  def updateSeason(optDates:Option[List[LocalDate]], s:Season, mailReport:Boolean)
 // def updateDb(updateData: List[GameMapping]):Future[Iterable[(Seq[Long], Seq[Long])]]
  def updateDb(keys:List[String],updateData: List[GameMapping]):Future[Iterable[(Seq[Long], Seq[Long])]]
}