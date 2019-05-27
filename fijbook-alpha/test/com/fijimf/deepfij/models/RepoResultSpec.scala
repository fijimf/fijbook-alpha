package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await

class RepoResultSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  "Results " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
    }

    "save a result " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
      val ss: Season = Await.result(dao.saveSeason(Season(0L, 2017)), testDbTimeout)
      val ts: List[Team] = Await.result(dao.saveTeams(List(
        Team(0L, "georgetown", "Georgetown", "Georgetown", "Hoyas", "Big East", None, None, None, None, None, None, None, LocalDateTime.now(), "me"),
        Team(0L, "st-johns","St. John's","St. John's","Red Storm","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me")
      )), testDbTimeout)
      private val date =  LocalDate.parse("2017-01-01")
      val gor: (Game, Option[Result]) = Await.result(dao.updateGameWithResult(Game(0L, ss.id, ts(0).id, ts(1).id, date, date.atStartOfDay(), None, isNeutralSite = false, None, None, None, "test", LocalDateTime.now(), "me"), None), testDbTimeout)

      val r: Result = Await.result(dao.saveResult(Result(0L, gor._1.id, 100, 95, 2, LocalDateTime.now(), "me")), testDbTimeout)
      assert(r.id>0)

    }
    "not save a bogus result " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)

      try {
        Await.result(dao.saveResult(Result(0L, 0L, 100, 95, 2, LocalDateTime.now(), "me")), testDbTimeout)
        fail("Result not mapped to a game")
      } catch {
        case e:Exception=> //OK
      }

    }

    "not save two results to a game " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
      val ss: Season = Await.result(dao.saveSeason(Season(0L, 2017)), testDbTimeout)
      val ts: List[Team] = Await.result(dao.saveTeams(List(
        Team(0L, "georgetown","Georgetown","Geogetown","Hoyas","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me"),
        Team(0L, "st-johns","St. John's","St. John's","Red Storm","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me")
      )), testDbTimeout)
      private val date =  LocalDate.parse("2017-01-01")
      val gor: (Game, Option[Result]) = Await.result(dao.updateGameWithResult(Game(0L, ss.id, ts(0).id, ts(1).id, date, date.atStartOfDay(), None, isNeutralSite = false, None, None, None, "test", LocalDateTime.now(), "me"), None), testDbTimeout)

      val r: Result = Await.result(dao.saveResult(Result(0L, gor._1.id, 100, 95, 2, LocalDateTime.now(), "me")), testDbTimeout)
      assert(r.id>0)
      val q: Result = Await.result(dao.saveResult(Result(0L, gor._1.id, 105, 95, 2, LocalDateTime.now(), "me")), testDbTimeout)
      assert(q.id===r.id)
      assert(Await.result(dao.listResults,testDbTimeout).size===1)

    }
  }
}
