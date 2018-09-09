package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDateTime

import com.fijimf.deepfij.models.{RssFeed, RssItem}

import scala.concurrent.Future

trait RssItemDAO {
  def findRssItemById(id: Long): Future[Option[RssItem]]

  def findRssItemsByFeed(feedId: Long): Future[List[RssItem]]

  def findRssItemsByDate(asOf: LocalDateTime, lookBackDays: Int):  Future[List[(RssItem, RssFeed)]]

  def saveRssItem(i: RssItem): Future[RssItem]

  def saveRssItems(is: List[RssItem]): Future[List[RssItem]]

  def deleteRssItem(id: Long): Future[Int]

  def deleteRssItemsByFeedId(feedId:Long): Future[Int]

  def listRssItems(): Future[List[RssItem]]
}
