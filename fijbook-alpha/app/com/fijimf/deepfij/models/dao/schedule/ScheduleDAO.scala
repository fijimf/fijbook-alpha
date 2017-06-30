package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._
import controllers.GameMapping

import scala.concurrent.Future

trait ScheduleDAO
  extends TeamDAO
    with SeasonDAO
    with AliasDAO
    with ResultDAO
    with GameDAO
    with QuoteDAO
    with ConferenceDAO
    with ConferenceMapDAO
    with StatValueDAO
    with GamePredictionDAO
    with LogisticModelDAO
    with UserProfileDAO {

  def updateScoreboard(updateData: List[GameMapping], sourceTag: String):Future[(Seq[Long], Seq[Long])]

  def loadSchedules(): Future[List[Schedule]]

  def loadLatestSchedule(): Future[Option[Schedule]]

}