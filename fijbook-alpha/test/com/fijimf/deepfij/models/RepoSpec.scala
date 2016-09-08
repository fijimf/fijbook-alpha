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


class RepoSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach {

  val repo = Injector.inject[Repo]

  override def afterEach() = EvolutionHelper.clean()

  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.seasons), Duration.Inf) mustBe List.empty
    }
    "allow seasons to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createSeason(2016)
      Await.result(id, Duration.Inf) must be > 0L
    }

    "prevent duplicate years from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createSeason(2016)
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createSeason(2016)
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a[SQLException]
      }
    }
  }

  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.teams), Duration.Inf) mustBe List.empty
    }
    "allow teams to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      Await.result(id, Duration.Inf) must be > 0L
    }
    "allow multiple teams to be created" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      private val v: Future[Long] = repo.createTeam("villanova", "Villanova", "Villanova University", "Wildcats")

      for (gId <- g;
           vId <- v) {
        gId must be > 0L
        vId must be > 0L
        gId must not equal vId
      }
    }
    "prevent duplicate keys from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createTeam("georgetown", "Villanova", "Villanova University", "Wildcats")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a[SQLException]
      }
    }
    "prevent duplicate names from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createTeam("villanova", "Georgetown", "Villanova University", "Wildcats")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a[SQLException]
      }
    }
  }

  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.conferences), Duration.Inf) mustBe List.empty
    }
    "allow conferences to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createConference("big-east", "Big East")
      Await.result(id, Duration.Inf) must be > 0L
    }
    "allow multiple conferences to be created" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createConference("big-east", "Big East")
      private val v: Future[Long] = repo.createConference("big-ten", "Big Ten")

      for (gId <- g;
           vId <- v) {
        gId must be > 0L
        vId must be > 0L
        gId must not equal vId
      }
    }
    "prevent duplicate keys from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createConference("big-east", "Big East")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createConference("big-east", "Big Ten")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a[SQLException]
      }
    }
    "prevent duplicate names from being inverted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createConference("big-east", "Big East")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createConference("big-ten", "Big East")
      ScalaFutures.whenReady(v.failed) { e =>
        e mustBe a[SQLException]
      }
    }
  }

  "ConferenceMaps " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.conferenceMaps), Duration.Inf) mustBe List.empty
    }
    "allow you to map a team to a conference for a season" in new WithApplication(FakeApplication()) {
      for (
        seasonId <- repo.createSeason(2016);
        confId <- repo.createConference("big-east", "Big East");
        teamId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")) {
        Await.result(repo.mapTeam(seasonId, teamId, confId), Duration.Inf) must be > 0L
      }
    }
    "ensure that a teams conference mappping is unique for a season" in new WithApplication(FakeApplication()) {
      for (
        seasonId <- repo.createSeason(2016);
        confId1 <- repo.createConference("big-east", "Big East");
        confId2 <- repo.createConference("a10", "Atlantic 10");
        teamId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas")) {
        Await.result(repo.mapTeam(seasonId, teamId, confId1), Duration.Inf) must be > 0L
        val v: Future[Long] = repo.mapTeam(seasonId, teamId, confId2)
        ScalaFutures.whenReady(v.failed) { e =>
          e mustBe a[SQLException]
        }
      }
    }
  }
  "Games " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.games), Duration.Inf) mustBe List.empty
    }
  }
  "Results " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.results), Duration.Inf) mustBe List.empty
    }
  }
}
