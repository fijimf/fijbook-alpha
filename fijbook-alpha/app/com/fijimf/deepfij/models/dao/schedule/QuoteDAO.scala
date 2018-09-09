package com.fijimf.deepfij.models.dao.schedule

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.Quote

import scala.concurrent.Future

trait QuoteDAO {

  def saveQuote(q: Quote): Future[Quote]

  def saveQuotes(qs: List[Quote]): Future[List[Quote]]

  def listQuotes: Future[List[Quote]]

  def findQuoteById(id: Long): Future[Option[Quote]]

  def findQuoteByKey(key: Option[String]): Future[List[Quote]]

  def deleteQuote(id: Long): Future[Int]

  def getWeeklyVoteCounts(asOf: LocalDate): Future[List[(Quote, Int, LocalDateTime)]]
}
