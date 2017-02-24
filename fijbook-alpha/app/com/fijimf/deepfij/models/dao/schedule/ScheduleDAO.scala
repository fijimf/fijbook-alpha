package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._

import scala.concurrent.Future

trait ScheduleDAO extends TeamDAO with SeasonDAO with AliasDAO with ResultDAO with GameDAO with QuoteDAO with ConferenceDAO with AnalyticsDAO {

  def loadSchedules(): Future[List[Schedule]]

  def loadLatestSchedule(): Future[Option[Schedule]]

}