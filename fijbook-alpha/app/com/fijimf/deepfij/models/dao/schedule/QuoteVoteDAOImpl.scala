package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{Quote, QuoteVote, ScheduleRepository}
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.Future
import scala.concurrent.duration.Duration


trait QuoteVoteDAOImpl extends QuoteVoteDAO with DAOSlick {

  val dbConfigProvider: DatabaseConfigProvider

  val repo: ScheduleRepository

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val JavaLocalDateTimeMapper: BaseColumnType[LocalDateTime]

  implicit val JavaLocalDateMapper: BaseColumnType[LocalDate]


  def saveQuoteVotes(qs: List[QuoteVote]): Future[List[QuoteVote]] =

    db.run(DBIO.sequence(qs.map(upsert)).transactionally)


  override def saveQuoteVote(q: QuoteVote): Future[QuoteVote] = db.run(upsert(q))

  private def upsert(x: QuoteVote) = {
    (repo.quoteVotes returning repo.quoteVotes.map(_.id)).insertOrUpdate(x).flatMap {
      case Some(id) => repo.quoteVotes.filter(_.id === id).result.head
      case None => DBIO.successful(x)
    }
  }


  override def listQuotesVotes: Future[List[QuoteVote]] = db.run(repo.quoteVotes.to[List].result)

  override def findQuoteVoteByUser(user: String, duration: Duration): Future[List[QuoteVote]] = {
    duration match {
      case Duration.Inf => db.run(repo.quoteVotes.filter(_.user === user).to[List].result)
      case d =>
        val t = LocalDateTime.now().minusNanos(duration.toNanos)
        db.run(repo.quoteVotes.filter(q => q.user === user && q.createdAt > t).to[List].result)
    }
  }

  override def deleteQuoteVote(id: Long): Future[Int] = db.run(repo.quoteVotes.filter(_.id === id).delete)

  override def deleteAllQuoteVotes(): Future[Int] = db.run(repo.quoteVotes.delete)

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
