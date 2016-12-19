package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.Schedule

import scala.concurrent.Future
import scala.language.postfixOps


trait StatisticWriterService {

  def update(): Option[Future[Option[Int]]]

  def updateForSchedule(sch: Schedule): Future[Option[Int]]

}