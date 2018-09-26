package com.fijimf.deepfij.models.dao.schedule

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{RssFeed, RssItem, ScheduleRepository}
import controllers.Utils._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait RssItemDAOImpl extends RssItemDAO with DAOSlick {

  val log: Logger
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def findRssItemById(id: Long): Future[Option[RssItem]] = {
    db.run(repo.rssItems.filter(_.id === id).result.headOption)
  }

  override def findRssItemsByFeed(feedId: Long): Future[List[RssItem]] = {
    val items = repo.rssItems

    val value = items.filter(_.rssFeedId === feedId)
    val eventualTypes = db.run(value.to[List].result)
    eventualTypes.onFailure{
      case thr=>
        log.error("",thr)
    }
    eventualTypes
  }

  override def findRssItemsByDate(asOf: LocalDateTime, lookBackDays: Int): Future[List[(RssItem, RssFeed)]] ={
    db.run(a = (for {
      item <- repo.rssItems
      feed <- repo.rssFeeds if feed.id === item.rssFeedId
    } yield {
      (item, feed)
    }).to[List].result).map(_.filter(_._1.publishTime.isBetween(asOf.minusDays(lookBackDays), asOf, inclusive = true)))
  }

  override def findRssItemsLike(key:String):  Future[List[(RssItem, RssFeed)]] = {
    val str = s"%${key.trim}%"
    db.run(a = (for {
      item <- repo.rssItems if item.title.like(str)
      feed <- repo.rssFeeds if feed.id === item.rssFeedId
    } yield {
      (item, feed)
    }).to[List].result)
  }


  override def saveRssItem(f: RssItem): Future[RssItem] = db.run(upsert(f))

  override def saveRssItems(fs: List[RssItem]): Future[List[RssItem]] = {
    db.run(DBIO.sequence(fs.map(upsert)).transactionally)
  }

  private def upsert(f: RssItem) = {
    (repo.rssItems returning repo.rssItems.map(_.id)).insertOrUpdate(f).flatMap {
      case Some(id) => repo.rssItems.filter(_.id === id).result.head
      case None => DBIO.successful(f)
    }
  }

  override def deleteRssItem(id: Long): Future[Int] = db.run(repo.rssItems.filter(_.id === id).delete)

  override def deleteRssItemsByFeedId(feedId: Long): Future[Int] = db.run(repo.rssItems.filter(_.rssFeedId === feedId).delete)

  override def listRssItems(): Future[List[RssItem]] = db.run(repo.rssItems.to[List].result)
}
