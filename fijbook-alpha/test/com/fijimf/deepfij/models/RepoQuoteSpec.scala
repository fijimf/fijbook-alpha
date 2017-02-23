package com.fijimf.deepfij.models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class RepoQuoteSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

  "Quotes " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listQuotes, Duration.Inf).isEmpty)
    }

    "be able to be inserted" in new WithApplication(FakeApplication()) {
      val q = Quote(0L, "The quote", None, None, None)
      val r = Await.result(dao.saveQuote(q), Duration.Inf)

      assert(r.id>0)
      assert(r.quote=="The quote")
      assert(Await.result(dao.listQuotes, Duration.Inf).size == 1)
    }

    "be able to be updated" in new WithApplication(FakeApplication()) {
      val q = Quote(0L, "The quote", None, None, None)
      val r = Await.result(dao.saveQuote(q), Duration.Inf)

      assert(r.id>0)
      assert(r.quote=="The quote")
      assert(Await.result(dao.listQuotes, Duration.Inf).size == 1)

      val s = Await.result(dao.saveQuote(r.copy(quote = "The new quote")), Duration.Inf)

      assert(s.id==r.id)
      assert(s.quote=="The new quote")
      assert(Await.result(dao.listQuotes, Duration.Inf).size == 1)
    }

    "be able to be retrieved by id" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, None)), Duration.Inf)
      val r = Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, None)), Duration.Inf)


      val s = Await.result(dao.findQuoteById(r.id), Duration.Inf)
      assert(s.isDefined)
      assert(s.get.quote=="The quote #4")

      val t = Await.result(dao.findQuoteById(-99), Duration.Inf)
      assert(t.isEmpty)
    }

    "be able to be retrieved by key" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, Some("A"))), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, Some("A"))), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, Some("A"))), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, Some("X"))), Duration.Inf)


      val s = Await.result(dao.findQuoteByKey(Some("A")), Duration.Inf)
      assert(s.size==3)
      val t = Await.result(dao.findQuoteByKey(Some("K")), Duration.Inf)
      assert(t.isEmpty)
      val u = Await.result(dao.findQuoteByKey(None), Duration.Inf)
      assert(u.size==1)

    }

    "be able to be deleted" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveQuote(Quote(0L, "The quote #1", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #2", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #3", None, None, None)), Duration.Inf)
      val r = Await.result(dao.saveQuote(Quote(0L, "The quote #4", None, None, None)), Duration.Inf)
      Await.result(dao.saveQuote(Quote(0L, "The quote #5", None, None, None)), Duration.Inf)


      val s = Await.result(dao.deleteQuote(r.id), Duration.Inf)
      assert(s==1)
      val t = Await.result(dao.deleteQuote(-99), Duration.Inf)
      assert(t==0)
      assert(Await.result(dao.listQuotes, Duration.Inf).size == 4)
    }
  }


}
