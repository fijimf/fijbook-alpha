package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class RepoQuoteSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach  with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "Quotes " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listQuotes, Duration.Inf).isEmpty)
    }

    "be able to be inserted" in new WithApplication(FakeApplication()) {
      val q = Quote(0L, "The quote", None, None, None)
      val r = Await.result(dao.saveQuote(q), testDbTimeout)

      assert(r.id>0)
      assert(r.quote=="The quote")
      assert(Await.result(dao.listQuotes, testDbTimeout).size == 1)
    }

    "be able to be updated" in new WithApplication(FakeApplication()) {
      val q = Quote(0L, "The quote", None, None, None)
      val r = Await.result(dao.saveQuote(q), testDbTimeout)

      assert(r.id>0)
      assert(r.quote=="The quote")
      assert(Await.result(dao.listQuotes, testDbTimeout).size == 1)

      val s = Await.result(dao.saveQuote(r.copy(quote = "The new quote")), testDbTimeout)

      assert(s.id==r.id)
      assert(s.quote=="The new quote")
      assert(Await.result(dao.listQuotes, testDbTimeout).size == 1)
    }

    "be able to be retrieved by id" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, None)), testDbTimeout)
      val r = Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, None)), testDbTimeout)


      val s = Await.result(dao.findQuoteById(r.id), testDbTimeout)
      assert(s.isDefined)
      assert(s.get.quote=="The quote #4")

      val t = Await.result(dao.findQuoteById(-99), testDbTimeout)
      assert(t.isEmpty)
    }

    "be able to be retrieved by key" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, Some("A"))), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, Some("A"))), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, Some("A"))), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, Some("X"))), testDbTimeout)


      val s = Await.result(dao.findQuoteByKey(Some("A")), testDbTimeout)
      assert(s.size==3)
      val t = Await.result(dao.findQuoteByKey(Some("K")), testDbTimeout)
      assert(t.isEmpty)
      val u = Await.result(dao.findQuoteByKey(None), testDbTimeout)
      assert(u.size==1)

    }

    "be able to be deleted" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, None)), testDbTimeout)
      val r = Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), testDbTimeout)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, None)), testDbTimeout)


      val s = Await.result(dao.deleteQuote(r.id), testDbTimeout)
      assert(s==1)
      val t = Await.result(dao.deleteQuote(-99), testDbTimeout)
      assert(t==0)
      assert(Await.result(dao.listQuotes, testDbTimeout).size == 4)
    }
  }


}
