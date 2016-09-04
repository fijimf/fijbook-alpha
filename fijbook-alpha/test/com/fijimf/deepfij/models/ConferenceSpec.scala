package com.fijimf.deepfij.models

import java.sql.SQLException

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.test._
import testhelpers.{EvolutionHelper, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class ConferenceSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach {

  val conferenceRepo = Injector.inject[ConferenceRepo]
  override def afterEach() = EvolutionHelper.clean()

  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(conferenceRepo.all, Duration.Inf) mustBe List.empty
    }
    "allow conferences to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = conferenceRepo.create("big-east", "Big East")
      Await.result(id, Duration.Inf) must be > 0L
    }
    "allow multiple conferences to be created" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = conferenceRepo.create("big-east", "Big East")
      private val v: Future[Long] = conferenceRepo.create("big-ten", "Big Ten")

      for (gId<-g;
           vId<-v) {
        gId must be > 0L
        vId must be > 0L
        gId must not equal vId
      }
    }
    "prevent duplicate keys from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = conferenceRepo.create("big-east", "Big East")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = conferenceRepo.create("big-east", "Big Ten")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a [SQLException]
      }
    }
    "prevent duplicate names from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = conferenceRepo.create("big-east", "Big East")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = conferenceRepo.create("big-ten", "Big East")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a [SQLException]
      }
    }
  }
}
