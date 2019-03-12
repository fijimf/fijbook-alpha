package com.fijimf.deepfij.models.services

import scala.concurrent.Future
import scala.language.postfixOps

trait ScheduleUpdateService {

  def update(str: String): Future[ScheduleUpdateResult]

  def update(sur: ScheduleUpdateRequest): Future[ScheduleUpdateResult]

  def verifyRecords(y: Int):Future[ResultsVerification]

}