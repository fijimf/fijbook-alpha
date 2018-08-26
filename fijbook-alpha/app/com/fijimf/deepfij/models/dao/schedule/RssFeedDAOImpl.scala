package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Quote, RssFeed, ScheduleRepository}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future

class RssFeedDAOImpl extends RssFeedDAO with DAOSlick {

  val log: Logger
  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  def findRssFeedById(id: Long): Future[Option[RssFeed]] = {
    db.run(repo.rssFeeds.filter(_.id === id).result.headOption)
  }

  override def saveRssFeed(f: RssFeed): Future[RssFeed] = db.run(
    (repo.rssFeeds returning repo.rssFeeds.map(_.id)).insertOrUpdate(f)
      .flatMap(i => {
        repo.rssFeeds.filter(ss => ss.id === i.getOrElse(f.id)).result.head
      })
  )

  override def saveRssFeeds(fs: List[RssFeed]): Future[List[RssFeed]] = {
    val ops = fs.map(f =>
      (repo.rssFeeds returning repo.rssFeeds.map(_.id)).insertOrUpdate(f).flatMap(ii => repo.rssFeeds.filter(_.id === ii).result.head)
    )
    db.run(DBIO.sequence(ops).transactionally)
  }

  override def deleteRssFeed(id: Long): Future[Int] = db.run(repo.rssFeeds.filter(_.id === id).delete)


}
