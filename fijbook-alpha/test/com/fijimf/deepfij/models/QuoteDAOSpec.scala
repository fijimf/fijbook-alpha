package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class QuoteDAOSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  "QuoteDAO " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listQuotes, testDbTimeout).isEmpty)
    }

    "be able to be inserted" in new WithApplication() {
      val r: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, None)), testDbTimeout)

      assert(r.id > 0)
      assert(r.quote === "The quote")
      assert(Await.result(dao.listQuotes, testDbTimeout).size === 1)
    }

    "be able to have quote updated" in new WithApplication() {
      val r: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, None)), testDbTimeout)
      val s: Quote = Await.result(dao.saveQuote(r.copy(quote = "The new quote")), testDbTimeout)

      assert(s.id === r.id)
      assert(s.quote === "The new quote")
      assert(s.source.isEmpty)
      assert(s.key.isEmpty)
      assert(s.url.isEmpty)
      assert(Await.result(dao.listQuotes, testDbTimeout).size === 1)
    }

    "be able to have source updated" in new WithApplication() {
      val x0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, None)), testDbTimeout)
      val x1: Quote = Await.result(dao.saveQuote(x0.copy(source = Some("src"))), testDbTimeout)
      assert(x1.id === x0.id)
      assert(x1.quote === x0.quote)
      assert(x1.source === Some("src"))
      assert(x1.key.isEmpty)
      assert(x1.url.isEmpty)

      val y0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", Some("sauce"), None, None)), testDbTimeout)
      val y1: Quote = Await.result(dao.saveQuote(y0.copy(source = None)), testDbTimeout)
      assert(y1.id === y0.id)
      assert(y1.quote === y0.quote)
      assert(y1.source.isEmpty)
      assert(y1.key.isEmpty)
      assert(y1.url.isEmpty)

      assert(Await.result(dao.listQuotes, testDbTimeout).size === 2)
    }

    "be able to have url updated" in new WithApplication() {
      val x0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, None)), testDbTimeout)
      val x1: Quote = Await.result(dao.saveQuote(x0.copy(url = Some("http://src"))), testDbTimeout)

      assert(x1.id === x0.id)
      assert(x1.quote === x0.quote)
      assert(x1.source.isEmpty)
      assert(x1.key.isEmpty)
      assert(x1.url === Some("http://src"))

      val y0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, Some("http://url"), None)), testDbTimeout)
      val y1: Quote = Await.result(dao.saveQuote(y0.copy(url = None)), testDbTimeout)

      assert(y1.id === y0.id)
      assert(y1.quote === y0.quote)
      assert(y1.source.isEmpty)
      assert(y1.key.isEmpty)
      assert(y1.url.isEmpty)

      assert(Await.result(dao.listQuotes, testDbTimeout).size === 2)
    }

    "be able to have key updated" in new WithApplication() {
      val x0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, None)), testDbTimeout)
      val x1: Quote = Await.result(dao.saveQuote(x0.copy(key = Some("yale"))), testDbTimeout)

      assert(x1.id === x0.id)
      assert(x1.quote === x0.quote)
      assert(x1.source.isEmpty)
      assert(x1.key === Some("yale"))
      assert(x1.url.isEmpty)

      val y0: Quote = Await.result(dao.saveQuote(Quote(0L, "The quote", None, None, Some("duke"))), testDbTimeout)
      val y1: Quote = Await.result(dao.saveQuote(y0.copy(key = None)), testDbTimeout)

      assert(y1.id === y0.id)
      assert(y1.quote === y0.quote)
      assert(y1.source.isEmpty)
      assert(y1.key.isEmpty)
      assert(y1.url.isEmpty)

      assert(Await.result(dao.listQuotes, testDbTimeout).size === 2)
    }

    "be saved in bulk" in new WithApplication() {

      val quotes: List[Quote] = Await.result(dao.saveQuotes(List(
        Quote(0L, "The quote #1", None, None, None),
        Quote(0L, "The quote #2", None, None, None),
        Quote(0L, "The quote #3", None, None, None),
        Quote(0L, "The quote #4", None, None, None),
        Quote(0L, "The quote #5", None, None, None)
      )), testDbTimeout)
      quotes.foreach(q => assert(q.id > 0))
      assert(Await.result(dao.listQuotes, testDbTimeout).size === 5)
    }

    "be able to be retrieved by id" in new WithApplication() {
      val quotes: List[Quote] = Await.result(dao.saveQuotes(List(
        Quote(0L, "The quote #1", None, None, None),
        Quote(0L, "The quote #2", None, None, None),
        Quote(0L, "The quote #3", None, None, None),
        Quote(0L, "The quote #4", None, None, None),
        Quote(0L, "The quote #5", None, None, None)
      )), testDbTimeout)

      quotes.foreach(q => {
        val r = Await.result(dao.findQuoteById(q.id), testDbTimeout)
        assert(r.isDefined)
        r.foreach(r1=>assert(r1 === q))
      })

      val s: Option[Quote] = Await.result(dao.findQuoteById(-99), testDbTimeout)
      assert(s.isEmpty)
    }

    "be able to be retrieved by key" in new WithApplication() {
      val quotes: List[Quote] = Await.result(dao.saveQuotes(List(
        Quote(0L, "The quote #1", None, None, Some("A")),
        Quote(0L, "The quote #2", None, None, Some("A")),
        Quote(0L, "The quote #3", None, None, Some("A")),
        Quote(0L, "The quote #4", None, None, None),
        Quote(0L, "The quote #5", None, None, Some("X"))
      )), testDbTimeout)

      val as: List[Quote] = Await.result(dao.findQuoteByKey(Some("A")), testDbTimeout)
      assert(as.size === 3)
      val ks: List[Quote] = Await.result(dao.findQuoteByKey(Some("K")), testDbTimeout)
      assert(ks.isEmpty)
      val us: List[Quote] = Await.result(dao.findQuoteByKey(None), testDbTimeout)
      assert(us.size === 1)

    }

    "be able to be deleted" in new WithApplication() {
      val quotes: List[Quote] = Await.result(dao.saveQuotes(List(
        Quote(0L, "The quote #1", None, None, None),
        Quote(0L, "The quote #2", None, None, None),
        Quote(0L, "The quote #3", None, None, None),
        Quote(0L, "The quote #4", None, None, None),
        Quote(0L, "The quote #5", None, None, None)
      )), testDbTimeout)

      assert(Await.result(dao.listQuotes, testDbTimeout).size === 5)

      val s: Int = Await.result(dao.deleteQuote(quotes(3).id), testDbTimeout)
      assert(s === 1)
      val t: Int = Await.result(dao.deleteQuote(-99), testDbTimeout)
      assert(t === 0)
      assert(Await.result(dao.listQuotes, testDbTimeout).size === 4)
    }
  }
}
