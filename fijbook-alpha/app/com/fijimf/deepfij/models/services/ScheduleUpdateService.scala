package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Season}
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {
  def update(optDates:Option[List[Int]], mailReport:Boolean)
  def updateSeason(optDates:Option[List[LocalDate]], s:Season, mailReport:Boolean)
  def updateDb(oldGameList: List[(Game, Option[Result])], updateData: List[GameMapping]):Future[Unit]
}