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
    with PredictionDAO
    with UserProfileDAO
    with QuoteVoteDAO
    with FavoriteLinkDAO
    with RssFeedDAO
    with RssItemDAO
    with JobDAO
    with JobRunDAO
    with CalcStatusDAO
    with ScoreboardDAO
{

  def loadSchedules(): Future[List[Schedule]]

  def loadSchedule(y:Int): Future[Option[Schedule]]

  def loadLatestSchedule(): Future[Option[Schedule]]

}