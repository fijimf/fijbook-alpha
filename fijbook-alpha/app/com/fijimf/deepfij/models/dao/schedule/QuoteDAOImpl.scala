package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Quote, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait QuoteDAOImpl extends QuoteDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

  override def findQuoteById(id: Long): Future[Option[Quote]] = db.run(repo.quotes.filter(_.id === id).result.headOption)

  override def findQuoteByKey(key: Option[String]): Future[List[Quote]] = key match {
    case Some(k)=>db.run(repo.quotes.filter(_.key === key).to[List].result)
    case None=>db.run(repo.quotes.filter(_.key.isEmpty).to[List].result)
  }

  override def saveQuote(q: Quote): Future[Quote] =     db.run(
    (repo.quotes returning repo.quotes.map(_.id)).insertOrUpdate(q)
      .flatMap(i => {
        repo.quotes.filter(ss => ss.id === i.getOrElse(q.id)).result.head
      })
  )

  override def deleteQuote(id: Long): Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

}
