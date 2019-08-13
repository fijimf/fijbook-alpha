package com.fijimf.deepfij.schedule.services

import com.fijimf.deepfij.models.services.ScheduleUpdateResult

import scala.concurrent.Future

trait ScheduleUpdateService {

  def update(str: String): Future[ScheduleUpdateResult]

  def update(sur: ScheduleUpdateRequest): Future[ScheduleUpdateResult]

  def verifyRecords(y: Int):Future[ResultsVerification]

}
