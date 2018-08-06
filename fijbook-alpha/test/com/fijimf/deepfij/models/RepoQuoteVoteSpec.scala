package com.fijimf.deepfij.models

import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.{Await, Future}


class RepoQuoteVoteSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "QuotesVotes " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listQuotesVotes, testDbTimeout).isEmpty)
    }

    "be able to be inserted" in new WithApplication() {
      val qv = QuoteVote(0L, 1L, "fijimf", LocalDateTime.now())
      val r = Await.result(dao.saveQuoteVote(qv), testDbTimeout)

      assert(r.id > 0)
      assert(r.quoteId === 1L)
      assert(Await.result(dao.listQuotesVotes, testDbTimeout).size == 1)
    }

    "be able to be retrieved " in new WithApplication() {

      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.duration._

      private val now: LocalDateTime = LocalDateTime.now()
      val qvs = List(QuoteVote(0L, 1L, "fijimf", now),
        QuoteVote(0L, 1L, "steve", now),
        QuoteVote(0L, 2L, "fijimf", now.minusDays(2))
      )
      val quoteVotes: List[QuoteVote] = Await.result(Future.sequence(qvs.map(dao.saveQuoteVote)),testDbTimeout)

      assert(quoteVotes.size ==3)
      assert(quoteVotes.forall(_.id>0L))

      val fijimfAllTimeVotes: List[QuoteVote] = Await.result(dao.findQuoteVoteByUser("fijimf"), testDbTimeout)
      assert(fijimfAllTimeVotes.size===2)
      assert(fijimfAllTimeVotes.forall(_.user==="fijimf"))

      val fijimfOneDayVotes: List[QuoteVote] = Await.result(dao.findQuoteVoteByUser("fijimf", 1.day), testDbTimeout)
      assert(fijimfOneDayVotes.size===1)
      assert(fijimfOneDayVotes.forall(_.user==="fijimf"))
      assert(fijimfOneDayVotes.forall(_.createdAt===now))

    }

    "be able to be all deleted" in new WithApplication() {
      import scala.concurrent.ExecutionContext.Implicits.global

      val qvs = List(QuoteVote(0L, 1L, "fijimf", LocalDateTime.now()),
        QuoteVote(0L, 1L, "steve", LocalDateTime.now()),
        QuoteVote(0L, 2L, "fijimf", LocalDateTime.now())
      )
      val quoteVotes: List[QuoteVote] = Await.result(Future.sequence(qvs.map(dao.saveQuoteVote)),testDbTimeout)

      assert(quoteVotes.size ==3)
      assert(quoteVotes.forall(_.id>0L))

      Await.ready(dao.deleteAllQuoteVotes(), testDbTimeout)

      val quoteVotes2: List[QuoteVote] = Await.result(dao.listQuotesVotes,testDbTimeout)

      assert(quoteVotes2.size===0)

    }
  }


}
