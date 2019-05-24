package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models._

import scala.concurrent.Future

trait ScheduleDAO
  extends TeamDAOImpl
    with GameDAOImpl
    with ResultDAOImpl
    with QuoteDAOImpl
    with SeasonDAOImpl
    with ConferenceDAOImpl
    with ConferenceMapDAOImpl
    with AliasDAOImpl
    with StatValueDAOImpl
    with PredictionDAOImpl
    with UserProfileDAOImpl
    with QuoteVoteDAOImpl
    with FavoriteLinkDAOImpl
    with RssFeedDAOImpl
    with RssItemDAOImpl
    with JobDAOImpl
    with JobRunDAOImpl
    with CalcStatusDAOImpl
    with ScoreboardDAOImpl
{

  def loadSchedules(): Future[List[Schedule]]

  def loadSchedule(y:Int): Future[Option[Schedule]]

  def loadLatestSchedule(): Future[Option[Schedule]]

}