package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.Season
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {

  def update(str: String):Future[String]

  def updateSeason(optDates: Option[List[LocalDate]]):Future[List[UpdateDbResult]]

  def updateSeason(optDates: Option[List[LocalDate]], s: Season):Future[List[UpdateDbResult]]

  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[UpdateDbResult]]

  def verifyRecords(y: Int):Future[ResultsVerification]

  def loadSeason(s:Season, tag:String): Future[List[UpdateDbResult]]
}