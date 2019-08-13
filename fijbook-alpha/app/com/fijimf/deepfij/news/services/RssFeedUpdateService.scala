package com.fijimf.deepfij.news.services

import com.fijimf.deepfij.models.RssItem

import scala.concurrent.Future

trait RssFeedUpdateService {

  def updateAllFeeds(): Future[List[RssItem]]
  def updateFeed(id:Long):Future[List[RssItem]]

}
