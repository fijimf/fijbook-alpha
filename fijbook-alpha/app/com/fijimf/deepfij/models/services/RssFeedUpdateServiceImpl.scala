package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.RssItem
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class RssFeedUpdateServiceImpl @Inject()(dao: ScheduleDAO)(implicit ec: ExecutionContext) extends RssFeedUpdateService  {
  override def updateFeed(id: Long): Future[List[RssItem]] = {
    dao.findRssFeedById(id).map(feed.map(_))
  }

}