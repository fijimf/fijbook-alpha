package com.fijimf.deepfij.models.services

import java.time.LocalDate

import com.fijimf.deepfij.models.Season
import controllers.GameMapping

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {
  case class UpdateDbResult(source: String, upserted: Seq[Long], deleted: Seq[Long])

  def updateSeason(optDates: Option[List[LocalDate]], mailReport: Boolean):Future[List[UpdateDbResult]]

  def updateSeason(optDates: Option[List[LocalDate]], s: Season, mailReport: Boolean):Future[List[UpdateDbResult]]

  def updateDb(keys: List[String], updateData: List[GameMapping]): Future[Iterable[UpdateDbResult]]

  def verifyRecords(y: Int):Future[ResultsVerification]

  def loadSeason(s:Season, tag:String): Future[List[UpdateDbResult]]
}