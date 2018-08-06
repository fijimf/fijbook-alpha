package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.DAOSlick
import com.fijimf.deepfij.models.{QuoteVote, ScheduleRepository}
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


  def saveQuoteVotes(qs: List[QuoteVote]): Future[List[QuoteVote]] = {
    val ops = qs.map(q =>
      (repo.quoteVotes returning repo.quoteVotes.map(_.id)).insertOrUpdate(q).flatMap(ii => repo.quoteVotes.filter(_.id === ii).result.head)
    )
    db.run(DBIO.sequence(ops).transactionally)
  }


  override def saveQuoteVote(q: QuoteVote): Future[QuoteVote] = saveQuoteVotes(List(q)).map(_.head)

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
}
