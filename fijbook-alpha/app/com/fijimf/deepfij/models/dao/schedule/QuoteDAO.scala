package com.fijimf.deepfij.models.dao.schedule

import com.fijimf.deepfij.models.Quote

import scala.concurrent.Future

trait QuoteDAO {

  def saveQuote(q: Quote): Future[Quote]

  def listQuotes: Future[List[Quote]]

  def findQuoteById(id: Long): Future[Option[Quote]]

  def findQuoteByKey(key: Option[String]): Future[List[Quote]]

  def deleteQuote(id: Long): Future[Int]
}
