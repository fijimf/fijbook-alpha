package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.RssItem

import scala.concurrent.Future

trait RssItemDAO {
  def findRssItemById(id: Long): Future[Option[RssItem]]

  def findRssItemsByFeed(feedId: Long): Future[List[RssItem]]

  def saveRssItem(i: RssItem): Future[RssItem]

  def saveRssItems(is: List[RssItem]): Future[List[RssItem]]

  def deleteRssItem(id: Long): Future[Int]
}
