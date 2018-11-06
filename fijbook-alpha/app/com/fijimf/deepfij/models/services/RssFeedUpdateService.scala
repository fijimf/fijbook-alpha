package com.fijimf.deepfij.models.services

import com.fijimf.deepfij.models.RssItem

import scala.concurrent.Future
import scala.language.postfixOps

trait RssFeedUpdateService {

  def updateAllFeeds(): Future[List[RssItem]]
  def updateFeed(id:Long):Future[List[RssItem]]

}