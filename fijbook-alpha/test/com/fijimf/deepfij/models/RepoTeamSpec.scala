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

class RepoTeamSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  import scala.concurrent.ExecutionContext.Implicits.global

  val dao = Injector.inject[ScheduleDAO]

  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
    }

    "return the new ID when inserted" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty
      private val t = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      private val s = Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(s,t)
    }

    "return the old ID when updated" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      private val s = Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(s,t)
      private val u = t.copy(id = s.id, nickname = "foos")
      private val v = Await.result(dao.saveTeam(u), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(u,v)
      private val w = Await.result(dao.listTeams, testDbTimeout).head
      assert(w.id == u.id)
      assert(w.id == v.id)
      assert(w.nickname == "foos")
    }

    "not be inserted with the same key as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "aaa", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      try {
        Await.result(dao.saveTeam(t2), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be updated with the same key as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "xxx", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id2 = Await.result(dao.saveTeam(t2), testDbTimeout)

      val t3 = t2.copy(id = id2.id, key = "aaa")
      try {
        Await.result(dao.saveTeam(t3), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be inserted with the same name as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "xxx", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      try {
        Await.result(dao.saveTeam(t2), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be updated with the same name as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "xxx", "Xxx", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id2 = Await.result(dao.saveTeam(t2), testDbTimeout)

      val t3 = t2.copy(id = id2.id, name = "Aaa")
      try {
        Await.result(dao.saveTeam(t3), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "find by an id" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val r = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(r), testDbTimeout)
      private val s = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val s1 = Await.result(dao.saveTeam(s), testDbTimeout)
      private val t = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(3)

      private val rr = Await.result(dao.findTeamById(s1.id), testDbTimeout)
      assert(rr.contains(s1))
    }

    "find by a key" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id = Await.result(dao.saveTeam(t2), testDbTimeout)
      private val t3 = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t3), testDbTimeout)

      private val rr = Await.result(dao.findTeamByKey("bbb"), testDbTimeout)
      assert(rr.get == id)
    }

    "delete by an id" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val t1 = Team(0L, "aaa", "Aaa", "Aaa", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      private val t2 = Team(0L, "bbb", "Bbb", "Bbb", "b1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      val id = Await.result(dao.saveTeam(t2), testDbTimeout)
      private val t3 = Team(0L, "ccc", "Ccc", "Ccc", "c1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
      Await.result(dao.saveTeam(t3), testDbTimeout)

      private val rr = Await.result(dao.deleteTeam(id.id), testDbTimeout)
      assert(rr==1)
      assert(Await.result(dao.listTeams, testDbTimeout).size == 2)

      private val ss = Await.result(dao.deleteTeam(-99), testDbTimeout)
      assert(ss==0)
      assert(Await.result(dao.listTeams, testDbTimeout).size == 2)

    }

    "handle multiple inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams = 0.to(500).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }
      ).toList
      Await.result(Future.sequence(teams), testDbTimeout)
    }

    "handle bulk inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams = 0.until(500).map(n => Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test") ).toList
      private val teams1 = Await.result(dao.saveTeams(teams), testDbTimeout)
      assertTeamsSize(500)
      assert(teams1.size == 500)
      private val teams2 = Await.result(dao.listTeams, testDbTimeout)
      assert(teams1.map(_.key).toSet == teams2.map(_.key).toSet)
    }

    "handle multiple concurrent inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams0 = 0.until(200).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      private val teams1 = 200.until(400).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "B", "BB", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList
      private val savedTeams = Await.result(Future.sequence(teams1++teams0), testDbTimeout)
      assert(savedTeams.size==400)
      assert(savedTeams.map(_.key).toSet == 0.until(400).map("team-" + _.toString).toSet)
      assert(savedTeams.map(_.name).toSet == 0.until(400).map("Team-" + _.toString).toSet)
    }

    "handle multiple concurrent bulk inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams0 = dao.saveTeams(
        0.until(200).map(n => Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")).toList
      )
      private val teams1 = dao.saveTeams(
        200.until(400).map(n => Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")).toList
      )
      private val t3: Future[List[Team]] = teams0.flatMap(ss=> teams1.map(_++ss))
      private val savedTeams = Await.result(t3, testDbTimeout)
      assert(savedTeams.size==400)
      assert(savedTeams.map(_.key).toSet == 0.until(400).map("team-" + _.toString).toSet)
      assert(savedTeams.map(_.name).toSet == 0.until(400).map("Team-" + _.toString).toSet)
    }

    "handle multiple inserts and updates" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams = 0.until(300).map(n => Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")).toList
      private val modTeams = dao.saveTeams(teams).flatMap(fts => dao.saveTeams(fts.map(_.copy(nickname = "New Nickname"))))
      private val result = Await.result(modTeams, testDbTimeout)
      assert(result.size == 300)
      assertTeamsSize(300)
    }

    "handle multiple concurrent inserts & updates & reads" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      private val teams0 = 0.until(500).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList

      private val teams1 = 500.until(800).map(n => {
        val t = Team(0L, "team-" + n.toString, "Team-" + n.toString, "A", "zzzzzzzzz", "c1", None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
        dao.saveTeam(t)
      }).toList

      private val teams2 = 0.until(800).map {
        case x if x % 2 == 0 => dao.findTeamById(x)
        case y => dao.findTeamByKey("team-" + y.toString)
      }.toList

      Await.result(Future.sequence(teams0++teams1++teams2), testDbTimeout)
    }


  }

  private def assertTeamsSize(size: Int) = {
    assert(Await.result(dao.listTeams, testDbTimeout).size == size)
  }

  private def assertTeamsIsEmpty() = {
    assert(Await.result(dao.listTeams, testDbTimeout).isEmpty,"'Teams' was not empty.")
  }

  private def compareTeams(s:Team, t:Team) = {
    assert(s.sameData(t),s"Team $s did not have the same data elements as $t")
  }

  "Quotes " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listQuotes, testDbTimeout).isEmpty)
    }
  }

  "Aliases " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listAliases, testDbTimeout).isEmpty)
    }
  }

  "Conferences " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, testDbTimeout).isEmpty)
    }
  }
  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, testDbTimeout).isEmpty)
    }
  }


}
