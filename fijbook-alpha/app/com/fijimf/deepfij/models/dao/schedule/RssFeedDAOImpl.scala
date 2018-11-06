package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, Quote, RssFeed, ScheduleRepository}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

trait RssFeedDAOImpl extends RssFeedDAO with DAOSlick {

  val log: Logger
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def findRssFeedById(id: Long): Future[Option[RssFeed]] = {
    db.run(repo.rssFeeds.filter(_.id === id).result.headOption)
  }

  override def saveRssFeed(f: RssFeed): Future[RssFeed] = db.run(upsert(f))

  override def saveRssFeeds(fs: List[RssFeed]): Future[List[RssFeed]] = {
    db.run(DBIO.sequence(fs.map(upsert)).transactionally)
  }

  private def upsert(x: RssFeed) = {
    (repo.rssFeeds returning repo.rssFeeds.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.rssFeeds.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  override def deleteRssFeed(id: Long): Future[Int] = db.run(repo.rssFeeds.filter(_.id === id).delete)

  override def listRssFeeds(): Future[List[RssFeed]] = db.run(repo.rssFeeds.to[List].result)
}
