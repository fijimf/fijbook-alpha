package com.fijimf.deepfij.models
import scala.concurrent.Future

/**
  * Created by jimfrohnhofer on 2/22/17.
  */
trait QuoteDAO {

  def saveQuote(q: Quote): Future[Int]

  def listQuotes: Future[List[Quote]]

  def findQuoteById(id: Long): Future[Option[Quote]]

  def findQuoteByKey(key: Option[String]): Future[List[Quote]]

  def deleteQuote(id: Long): Future[Int]
}
