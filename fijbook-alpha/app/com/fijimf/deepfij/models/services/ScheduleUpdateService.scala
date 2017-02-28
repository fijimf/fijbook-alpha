package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, Result, Season}
import controllers.GameMapping

import scala.language.postfixOps

trait ScheduleUpdateService {
  def update(optDates:Option[List[LocalDate]], mailReport:Boolean)
  def updateSeason(optDates:Option[List[LocalDate]], s:Season, mailReport:Boolean)
  def updateDb(oldGameList: List[(Game, Option[Result])], updateData: List[GameMapping]):Unit
}