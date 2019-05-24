package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import akka.actor.ActorSystem
import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Alias, Quote, QuoteVote, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future


trait QuoteDAO extends DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

   def listQuotes: Future[List[Quote]] = db.run(repo.quotes.to[List].result)

   def findQuoteById(id: Long): Future[Option[Quote]] = db.run(repo.quotes.filter(_.id === id).result.headOption)

   def findQuoteByKey(key: Option[String]): Future[List[Quote]] = key match {
    case Some(k) => db.run(repo.quotes.filter(_.key === key).to[List].result)
    case None => db.run(repo.quotes.filter(_.key.isEmpty).to[List].result)
  }

   def findQuotesLike(key:String): Future[List[Quote]] = {
    val str=s"%${key.trim}%"
    db.run(
      repo.quotes.filter(q => q.quote.like(str) || q.source.like(str)).to[List].result
    )
  }

   def saveQuote(q: Quote): Future[Quote] = db.run(upsert(q))

   def saveQuotes(qs: List[Quote]): Future[List[Quote]] = {
    db.run(DBIO.sequence(qs.map(upsert)).transactionally)
  }

  private def upsert(x: Quote) = {
    (repo.quotes returning repo.quotes.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.quotes.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


   def deleteQuote(id: Long): Future[Int] = db.run(repo.quotes.filter(_.id === id).delete)

   def getWeeklyVoteCounts(asOf: LocalDate): Future[List[(Quote, Int, LocalDateTime)]] = {
    import controllers.Utils._
    val fzs: Future[List[(Quote, QuoteVote)]] = db.run((for {
      q <- repo.quotes
      qv <- repo.quoteVotes if qv.quoteId === q.id
    } yield {
      q -> qv
    }).to[List].result)

    fzs.map(zs => {
      val grouped = zs.groupBy(_._1)
      grouped.map { case (q, vals) =>
        val date = vals.maxBy(_._2.createdAt.toMillis)._2.createdAt
        val count = vals.count(_._2.createdAt.isAfter(asOf.atStartOfDay().minusWeeks(1)))
        (q, count, date)
      }
    }.toList)
  }
}
