package com.fijimf.deepfij.models.services

import java.time.LocalDate

import scala.language.postfixOps

trait ScheduleUpdateService {
  def update(optDates:Option[List[LocalDate]])
}