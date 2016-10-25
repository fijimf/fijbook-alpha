package com.fijimf.deepfij.models

import java.sql.SQLException

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class RepoSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

  "Seasons " should {

    "dump schema when asked" in new WithApplication(FakeApplication()) {
      Await.result(repo.dumpSchema(), Duration.Inf)._1.foreach(s=>println(s+";"))
    }

    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.seasons), Duration.Inf).isEmpty)
    }
    "allow seasons to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createSeason(2016)
      assert(Await.result(id, Duration.Inf) > 0L)
    }

    "prevent duplicate years from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createSeason(2016)
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createSeason(2016)
      whenReady(v.failed) { e =>
        assert(e.isInstanceOf[SQLException])
      }
    }
  }

  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.teams), Duration.Inf).isEmpty)
    }
    "allow teams to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER")
      assert(Await.result(id, Duration.Inf) > 0L)
    }
    "allow multiple teams to be created" in new WithApplication(FakeApplication()) {
      val (gg, vv) = Await.result(for (gId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER");
                                       vId <- repo.createTeam("villanova", "Villanova", "Villanova University", "Wildcats", updatedBy="TESTUSER"))
        yield { (gId, vId)}, Duration.Inf)
      assert(gg > 0L)
      assert(vv > 0L)
      assert(gg != vv)
    }
    "prevent duplicate keys from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createTeam("georgetown", "Villanova", "Villanova University", "Wildcats", updatedBy="TESTUSER")
      whenReady(v.failed) { e =>
        assert(e.isInstanceOf[SQLException])
      }
    }
    "prevent duplicate names from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createTeam("villanova", "Georgetown", "Villanova University", "Wildcats", updatedBy="TESTUSER")
      whenReady(v.failed) { e =>
        assert(e.isInstanceOf[SQLException])
      }
    }
    "can retrieve all teams as a map" in new WithApplication(FakeApplication()) {
      Await.result(for (gId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER");
                        vId <- repo.createTeam("villanova", "Villanova", "Villanova University", "Wildcats", updatedBy="TESTUSER"))
        yield {
          (gId, vId)
        }, Duration.Inf)
      private val teams: Map[String, Team] = Await.result(repo.getTeams, Duration.Inf)
      assert(teams.get("villanova").map(_.longName).contains("Villanova University"))
      assert(teams.get("syracuse").map(_.longName).isEmpty)
    }
  }


  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.conferences), Duration.Inf).isEmpty)
    }
    "allow conferences to be created" in new WithApplication(FakeApplication()) {
      private val id: Future[Long] = repo.createConference("big-east", "Big East", updatedBy="TESTUSER")
      assert(Await.result(id, Duration.Inf) > 0L)
    }
    "allow multiple conferences to be created" in new WithApplication(FakeApplication()) {
      val (g,v) = Await.result(for (gId <- repo.createConference("big-east", "Big East", updatedBy="TESTUSER");
                                    vId <- repo.createConference("big-ten", "Big Ten", updatedBy="TESTUSER"))
        yield {
          (gId, vId)
        }, Duration.Inf)
      assert(g > 0L)
      assert(v > 0L)
      assert(g != v)
    }

    "prevent duplicate keys from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createConference("big-east", "Big East", updatedBy="TESTUSER")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createConference("big-east", "Big Ten", updatedBy="TESTUSER")
      whenReady(v.failed) { e =>
        assert(e.isInstanceOf[SQLException])
      }
    }
    "prevent duplicate names from being inserted" in new WithApplication(FakeApplication()) {
      private val g: Future[Long] = repo.createConference("big-east", "Big East", updatedBy="TESTUSER")
      Await.result(g, Duration.Inf)
      private val v: Future[Long] = repo.createConference("big-ten", "Big East", updatedBy="TESTUSER")
      whenReady(v.failed) { e =>
        assert(e.isInstanceOf[SQLException])
      }
    }
  }

  "ConferenceMaps " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.conferenceMaps), Duration.Inf).isEmpty)
    }
    "allow you to map a team to a conference for a season" in new WithApplication(FakeApplication()) {
      assert(Await.result(for (
        seasonId <- repo.createSeason(2016);
        confId <- repo.createConference("big-east", "Big East", updatedBy="TESTUSER");
        teamId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER");
        cmId <- repo.mapTeam(seasonId, teamId, confId, updatedBy="TESTUSER")) yield {
        cmId
      }, Duration.Inf) > 0L)
    }
    "ensure that a teams conference mappping is unique for a season" in new WithApplication(FakeApplication()) {
      val (s, c1, c2, t) = Await.result(for (
        seasonId <- repo.createSeason(2016);
        confId1 <- repo.createConference("big-east", "Big East", updatedBy="TESTUSER");
        confId2 <- repo.createConference("a10", "Atlantic 10", updatedBy="TESTUSER");
        teamId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", updatedBy="TESTUSER")) yield {
        (seasonId, confId1, confId2, teamId)
      }, Duration.Inf)


      val cmId1 = Await.result(repo.mapTeam(s, t, c1, updatedBy="TESTUSER"), Duration.Inf)
      assert(cmId1 > 0L)

      val cmId2 = Await.result(repo.mapTeam(s, t, c2, updatedBy="TESTUSER"), Duration.Inf)
      assert(cmId2 > 0L)
      assert(cmId1 == cmId2)

      assert(Await.result(repo.all(repo.conferenceMaps), Duration.Inf).size == 1)

    }
  }

  "Games " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.games), Duration.Inf).isEmpty)
    }
  }
  "Results " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(repo.all(repo.results), Duration.Inf).isEmpty)
    }
  }
}
