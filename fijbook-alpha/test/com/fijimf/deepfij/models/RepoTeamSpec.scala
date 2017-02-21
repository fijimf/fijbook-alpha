package com.fijimf.deepfij.models

import java.time.LocalDateTime

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class RepoTeamSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }


  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listTeams, Duration.Inf).isEmpty)
    }

    "return the new ID when inserted" in new WithApplication(FakeApplication()) {
      private val t = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      private val fi = Await.result(dao.saveTeam(t), Duration.Inf)
      assert(Await.result(dao.listTeams, Duration.Inf).size == 1)
      assert(fi.id > 0)
    }

    "return the old ID when updated" in new WithApplication(FakeApplication()) {
      private val t = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      private val fi = Await.result(dao.saveTeam(t), Duration.Inf)
      assert(Await.result(dao.listTeams, Duration.Inf).size == 1)
      assert(fi.id > 0)
      private val s = t.copy(id = fi.id, nickname = "foos")
      private val fj = Await.result(dao.saveTeam(s), Duration.Inf)
      private val teamList = Await.result(dao.listTeams, Duration.Inf)
      assert(teamList.size == 1)
      assert(teamList.head.id == fi.id)
      assert(teamList.head.id == fj.id)
      assert(teamList.head.nickname == "foos")
    }

    "not be inserted with the same key as an existing team" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "aaa", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      try {
        Await.result(dao.saveTeam(t2), Duration.Inf)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be updated with the same key as an existing team" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "xxx", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id2 = Await.result(dao.saveTeam(t2), Duration.Inf)

      val t3 = t2.copy(id = id2.id, key = "aaa")
      try {
        Await.result(dao.saveTeam(t3), Duration.Inf)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be inserted with the same name as an existing team" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "xxx", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      try {
        Await.result(dao.saveTeam(t2), Duration.Inf)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be updated with the same name as an existing team" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "xxx", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id2 = Await.result(dao.saveTeam(t2), Duration.Inf)

      val t3 = t2.copy(id = id2.id, name = "Aaa")
      try {
        Await.result(dao.saveTeam(t3), Duration.Inf)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "find by an id" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id = Await.result(dao.saveTeam(t2), Duration.Inf)
      private val t3 = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t3), Duration.Inf)

      private val rr = Await.result(dao.findTeamById(id.id), Duration.Inf)
      assert(rr.get == id)
    }

    "find by an key" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id = Await.result(dao.saveTeam(t2), Duration.Inf)
      private val t3 = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t3), Duration.Inf)

      private val rr = Await.result(dao.findTeamByKey("bbb"), Duration.Inf)
      assert(rr.get == id)
    }

    "delete by an id" in new WithApplication(FakeApplication()) {
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), Duration.Inf)
      private val t2 = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id = Await.result(dao.saveTeam(t2), Duration.Inf)
      private val t3 = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t3), Duration.Inf)

      private val rr = Await.result(dao.deleteTeam(id.id), Duration.Inf)
      assert(rr==1)
      assert(Await.result(dao.listTeams, Duration.Inf).size == 2)

      private val ss = Await.result(dao.deleteTeam(-99), Duration.Inf)
      assert(ss==0)
      assert(Await.result(dao.listTeams, Duration.Inf).size == 2)



    }

    "handle multiple concurrent inserts" in new WithApplication(FakeApplication()) {
      import scala.concurrent.ExecutionContext.Implicits.global
      private val teams = 0.to(1500).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }
      ).toList
      Await.result(Future.sequence(teams), Duration.Inf)
    }

    "handle multiple concurrent inserts & updates" in new WithApplication(FakeApplication()) {
      import scala.concurrent.ExecutionContext.Implicits.global
      private val teams0 = 0.to(1500).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      private val teams1 = 500.to(1800).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "zzzzzzzzz", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      Await.result(Future.sequence(teams1++teams0), Duration.Inf)
    }

    "handle multiple concurrent inserts & updates & reads" in new WithApplication(FakeApplication()) {
      import scala.concurrent.ExecutionContext.Implicits.global
      private val teams0 = 0.to(1500).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList

      private val teams1 = 500.to(1800).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "zzzzzzzzz", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList

      private val teams2 = 0.to(1800).map {
        case x if x % 2 == 0 => dao.findTeamById(x)
        case y => dao.findTeamByKey("team-" + y.toString)
      }.toList

      Await.result(Future.sequence(teams0++teams1++teams2), Duration.Inf)
    }


  }

  "Quotes " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listQuotes, Duration.Inf).isEmpty)
    }
  }

  "Aliases " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listAliases, Duration.Inf).isEmpty)
    }
  }

  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, Duration.Inf).isEmpty)
    }
  }
  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, Duration.Inf).isEmpty)
    }
  }


}