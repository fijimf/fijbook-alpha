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


//  "Teams " should {
//    "be empty initially" in new WithApplication(FakeApplication()) {
//      assert(Await.result(repo.all(repo.teams), Duration.Inf).isEmpty)
//    }
//    "allow teams to be created" in new WithApplication(FakeApplication()) {
//      private val id: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas",  "Big East",updatedBy="TESTUSER")
//      assert(Await.result(id, Duration.Inf) > 0L)
//    }
//    "allow multiple teams to be created" in new WithApplication(FakeApplication()) {
//      val (gg, vv) = Await.result(for (gId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas",  "Big East",updatedBy="TESTUSER");
//                                       vId <- repo.createTeam("villanova", "Villanova", "Villanova University", "Wildcats",  "Big East",updatedBy="TESTUSER"))
//        yield { (gId, vId)}, Duration.Inf)
//      assert(gg > 0L)
//      assert(vv > 0L)
//      assert(gg != vv)
//    }
//    "prevent duplicate keys from being inserted" in new WithApplication(FakeApplication()) {
//      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", "Big East", updatedBy="TESTUSER")
//      Await.result(g, Duration.Inf)
//      private val v: Future[Long] = repo.createTeam("georgetown", "Villanova", "Villanova University", "Wildcats", "Big East", updatedBy="TESTUSER")
//      whenReady(v.failed) { e =>
//        assert(e.isInstanceOf[SQLException])
//      }
//    }
//    "prevent duplicate names from being inserted" in new WithApplication(FakeApplication()) {
//      private val g: Future[Long] = repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas", "Big East", updatedBy="TESTUSER")
//      Await.result(g, Duration.Inf)
//      private val v: Future[Long] = repo.createTeam("villanova", "Georgetown", "Villanova University", "Wildcats",  "Big East",updatedBy="TESTUSER")
//      whenReady(v.failed) { e =>
//        assert(e.isInstanceOf[SQLException])
//      }
//    }
//    "can retrieve all teams as a map" in new WithApplication(FakeApplication()) {
//      Await.result(for (gId <- repo.createTeam("georgetown", "Georgetown", "Georgetown University", "Hoyas",  "Big East",updatedBy="TESTUSER");
//                        vId <- repo.createTeam("villanova", "Villanova", "Villanova University", "Wildcats",  "Big East",updatedBy="TESTUSER"))
//        yield {
//          (gId, vId)
//        }, Duration.Inf)
//      private val teams: Map[String, Team] = Await.result(repo.getTeams, Duration.Inf)
//      assert(teams.get("villanova").map(_.longName).contains("Villanova University"))
//      assert(teams.get("syracuse").map(_.longName).isEmpty)
//    }
//  }


}
