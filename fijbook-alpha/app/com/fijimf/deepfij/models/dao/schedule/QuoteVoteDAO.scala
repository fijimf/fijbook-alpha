package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.QuoteVote

import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait QuoteVoteDAO {

  def saveQuoteVote(q: QuoteVote): Future[QuoteVote]

  def listQuotesVotes: Future[List[QuoteVote]]

  def findQuoteVoteByUser(user: String, last: Duration = Duration.Inf): Future[List[QuoteVote]]

  def deleteQuoteVote(id: Long): Future[Int]

  def deleteAllQuoteVotes(): Future[Int]
}
