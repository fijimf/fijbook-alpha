package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Quote, ScheduleRepository}

import scala.concurrent.Future


trait QuoteDAO extends DAOSlick {

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  val repo: ScheduleRepository

  def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  def findQuoteById(id: Long): Future[Option[Quote]] = db.run(repo.quotes.filter(_.id === id).result.headOption)

  def findQuoteByKey(key: Option[String]): Future[List[Quote]] =
    db.run(repo.quotes.filter(q=>q.key === key || (q.key.isEmpty && key.isEmpty)).to[List].result)

  def findQuotesLike(key:String): Future[List[Quote]] = {
    val str=s"%${key.trim}%"
    db.run(repo.quotes.filter(q => q.quote.like(str) || q.source.like(str)).to[List].result)
  }

  def saveQuote(q: Quote): Future[Quote] = db.run(upsert(q))

  def saveQuotes(qs: List[Quote]): Future[List[Quote]] = db.run(DBIO.sequence(qs.map(upsert)).transactionally)

  def deleteQuote(id: Long): Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

  private def upsert(x: Quote) = {
    (repo.quotes returning repo.quotes.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.quotes.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }

}
