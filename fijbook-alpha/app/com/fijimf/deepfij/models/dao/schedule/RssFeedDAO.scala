package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.RssFeed

import scala.concurrent.Future

trait RssFeedDAO {
  def findRssFeedById(id: Long): Future[Option[RssFeed]]

  def saveRssFeed(f: RssFeed): Future[RssFeed]

  def saveRssFeeds(fs: List[RssFeed]): Future[List[RssFeed]]

  def deleteRssFeed(id: Long): Future[Int]
}
