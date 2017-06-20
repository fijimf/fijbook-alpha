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
      assertTeamsIsEmpty()
      val t = mkQuickTeam(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "Big East")
      val s = Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(s, t)
    }

    "return the old ID when updated" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t = mkQuickTeam(0L, "uconn", "UConn", "Connecticut", "Huskies", "Big East")
      val s = Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(s, t)
      val u = t.copy(id = s.id, optConference = "American")
      val v = Await.result(dao.saveTeam(u), testDbTimeout)
      assertTeamsSize(1)
      compareTeams(u, v)
      val w = Await.result(dao.listTeams, testDbTimeout).head
      assert(w.id == u.id)
      assert(w.id == v.id)
      assert(w.optConference == "American")
    }

    "not be inserted with the same key as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "usc", "South Carolina", "South Carolina", "Gamecocks", "SEC")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "usc", "USC", "Southern California", "Trojans", "Pac-12")
      try {
        Await.result(dao.saveTeam(t2), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be updated with the same key as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "usc", "USC", "Southern California", "Trojans", "Pac-12")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "s-car", "South Carolina", "South Carolina", "Gamecocks", "SEC")
      val id2 = Await.result(dao.saveTeam(t2), testDbTimeout)

      val t3 = t2.copy(id = id2.id, key = "usc")
      try {
        Await.result(dao.saveTeam(t3), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be inserted with the same name as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "usc", "USC", "Southern California", "Trojans", "Pac-12")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "s-car", "USC", "South Carolina", "Gamecocks", "SEC")
      try {
        Await.result(dao.saveTeam(t2), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
      }
    }

    "not be bulk inserted with the same name as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val ts = List(
        mkQuickTeam(0L, "usc", "USC", "Southern California", "Trojans", "Pac-12"),
        mkQuickTeam(0L, "s-car", "USC", "South Carolina", "Gamecocks", "SEC")
      )
      try {
        Await.result(dao.saveTeams(ts), testDbTimeout)
        fail
      } catch {
        case _: Throwable => //OK
          assertTeamsIsEmpty() //saveTeams is transactional
      }
    }

    "not be updated with the same name as an existing team" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "aaa", "Aaa", "Aaa", "a1s", "c1")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "xxx", "Xxx", "Aaa", "a1s", "c1")
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
      val r = mkQuickTeam(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "Big East")
      Await.result(dao.saveTeam(r), testDbTimeout)
      val s = mkQuickTeam(0L, "villanova", "Villanova", "Villanova", "Wildcats", "Big East")
      val s1 = Await.result(dao.saveTeam(s), testDbTimeout)
      val t = mkQuickTeam(0L, "xavier", "Xavier", "Xavier", "Musketeers", "Big East")
      Await.result(dao.saveTeam(t), testDbTimeout)
      assertTeamsSize(3)

      val rr = Await.result(dao.findTeamById(s1.id), testDbTimeout)
      assert(rr.contains(s1))
    }

    "find by a key" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "Big East")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "villanova", "Villanova", "Villanova", "Wildcats", "Big East")
      val nova = Await.result(dao.saveTeam(t2), testDbTimeout)
      val t3 = mkQuickTeam(0L, "xavier", "Xavier", "Xavier", "Musketeers", "Big East")
      Await.result(dao.saveTeam(t3), testDbTimeout)

      val rr = Await.result(dao.findTeamByKey("villanova"), testDbTimeout)
      assert(rr.get == nova)
    }

    "delete by an id" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val t1 = mkQuickTeam(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "Big East")
      Await.result(dao.saveTeam(t1), testDbTimeout)
      val t2 = mkQuickTeam(0L, "villanova", "Villanova", "Villanova", "Wildcats", "Big East")
      val nova = Await.result(dao.saveTeam(t2), testDbTimeout)
      val t3 = mkQuickTeam(0L, "xavier", "Xavier", "Xavier", "Musketeers", "Big East")
      Await.result(dao.saveTeam(t3), testDbTimeout)

      val numRows1 = Await.result(dao.deleteTeam(nova.id), testDbTimeout)
      assert(numRows1 == 1)
      assert(Await.result(dao.listTeams, testDbTimeout).size == 2)
      val rr = Await.result(dao.findTeamByKey("villanova"), testDbTimeout)
      assert(rr == None)

      val numRows2 = Await.result(dao.deleteTeam(-99), testDbTimeout)
      assert(numRows2 == 0)
      assert(Await.result(dao.listTeams, testDbTimeout).size == 2)

    }

    "handle multiple inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams = 0.to(500).map(n => {
        val t = mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1")
        dao.saveTeam(t)
      }
      ).toList
      Await.result(Future.sequence(teams), testDbTimeout)
    }

    "handle bulk inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams = 0.until(500).map(n => mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1")).toList
      val teams1 = Await.result(dao.saveTeams(teams), testDbTimeout)
      assertTeamsSize(500)
      assert(teams1.size == 500)
      val teams2 = Await.result(dao.listTeams, testDbTimeout)
      assert(teams1.map(_.key).toSet == teams2.map(_.key).toSet)
    }

    "handle multiple concurrent inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams0 = 0.until(200).map(n => {
        val t = mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1")
        dao.saveTeam(t)
      }).toList
      val teams1 = 200.until(400).map(n => {
        val t = mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "B", "BB", "c1")
        dao.saveTeam(t)
      }).toList
      val savedTeams = Await.result(Future.sequence(teams1 ++ teams0), testDbTimeout)
      assert(savedTeams.size == 400)
      assert(savedTeams.map(_.key).toSet == 0.until(400).map("team-" + _.toString).toSet)
      assert(savedTeams.map(_.name).toSet == 0.until(400).map("Team-" + _.toString).toSet)
    }

    "handle multiple concurrent bulk inserts" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams0 = dao.saveTeams(
        0.until(200).map(n => mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1")).toList
      )
      val teams1 = dao.saveTeams(
        200.until(400).map(n => mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "AA", "c1")).toList
      )
      val t3: Future[List[Team]] = teams0.flatMap(ss => teams1.map(_ ++ ss))
      val savedTeams = Await.result(t3, testDbTimeout)
      assert(savedTeams.size == 400)
      assert(savedTeams.map(_.key).toSet == 0.until(400).map("team-" + _.toString).toSet)
      assert(savedTeams.map(_.name).toSet == 0.until(400).map("Team-" + _.toString).toSet)
    }

    "handle multiple inserts and updates" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams = 0.until(300).map(n => mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1")).toList
      val modTeams = dao.saveTeams(teams).flatMap(fts => dao.saveTeams(fts.map(_.copy(nickname = "New Nickname"))))
      val result = Await.result(modTeams, testDbTimeout)
      assert(result.size == 300)
      assertTeamsSize(300)
    }

    "handle multiple concurrent inserts & updates & reads" in new WithApplication(FakeApplication()) {
      assertTeamsIsEmpty()
      val teams0 = 0.until(500).map(n => {
        val t = mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "a1s", "c1")
        dao.saveTeam(t)
      }).toList

      val teams1 = dao.saveTeams(500.until(800).map(n => mkQuickTeam(0L, "team-" + n.toString, "Team-" + n.toString, "A", "zzzzzzzzz", "c1")).toList)

      val teams2 = 0.until(800).map {
        case x if x % 2 == 0 => dao.findTeamById(x)
        case y => dao.findTeamByKey("team-" + y.toString)
      }.toList

      Await.result(Future.sequence(teams1 :: (teams0 ++ teams2)), testDbTimeout)

      assertTeamsSize(800)
    }


  }

  def mkQuickTeam(id: Long, key: String, name: String, longName: String, nickname: String, optConf: String): Team = {
    Team(id, key, name, longName, nickname, optConf, None, None, None, None, None, None, None, LocalDateTime.now(), "Test")
  }


  def assertTeamsSize(size: Int) = {
    assert(Await.result(dao.listTeams, testDbTimeout).size == size)
  }

  def assertTeamsIsEmpty() = {
    assert(Await.result(dao.listTeams, testDbTimeout).isEmpty, "'Teams' was not empty.")
  }

  def compareTeams(s: Team, t: Team) = {
    assert(s.sameData(t), s"Team $s did not have the same data elements as $t")
  }



  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferences, testDbTimeout).isEmpty)
    }
  }


}
