package com.fijimf.deepfij.models

import java.sql.SQLException

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._
import testhelpers.{EvolutionHelper, Injector}

import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class TeamSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach {

  val teamRepo = Injector.inject[TeamRepo]
  override def afterEach() = EvolutionHelper.clean()

  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(teamRepo.all, Duration.Inf) mustBe List.empty
    }
    "allow teams to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = teamRepo.create("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      Await.result(id, Duration.Inf) must be > 0L
    }
    "allow multiple teams to be created" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = teamRepo.create("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      private val v: Future[Long] = teamRepo.create("villanova", "Villanova", "Villanova University", "Wildcats")

      for (gId<-g;
           vId<-v) {
        gId must be > 0L
        vId must be > 0L
        gId must not equal vId
      }
    }
    "prevent duplicate keys from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = teamRepo.create("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      private val v: Future[Long] = teamRepo.create("georgetown", "Villanova", "Villanova University", "Wildcats")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a [SQLException]
      }
    }
    "prevent duplicate names from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = teamRepo.create("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      private val v: Future[Long] = teamRepo.create("villanova", "Georgetown", "Villanova University", "Wildcats")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a [SQLException]
      }
    }
  }
}
