package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._

class RepoResultSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "Results " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
    }

    "save a result " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
      val ss = Await.result(dao.saveSeason(Season(0L, 2017,"", None)), testDbTimeout)
      val ts = Await.result(dao.saveTeams(List(
        Team(0L, "georgetown","Georgetown","Geogetown","Hoyas","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me"),
        Team(0L, "st-johns","St. John's","St. John's","Red Storm","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me")
      )), testDbTimeout)
      private val date =  LocalDate.parse("2017-01-01")
      val gId = Await.result(dao.saveGame(Game(0L, ss.id,ts(0).id,ts(1).id,date, date.atStartOfDay(),None, false,None,None,None,"test",LocalDateTime.now(),"me"),None),testDbTimeout)

      val r = Await.result(dao.saveResult(Result(0L,gId,100,95,2,LocalDateTime.now(),"me")), testDbTimeout)
      assert(r.id>0)

    }
    "not save a bogus result " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)

      try {
        val r = Await.result(dao.saveResult(Result(0L,0L,100,95,2,LocalDateTime.now(),"me")), testDbTimeout)
        fail("Result not mapped to a game")
      } catch {
        case e:Exception=> //OK
      }

    }

    "not save two results to a game " in new WithApplication() {
      assert(Await.result(dao.listResults, testDbTimeout).isEmpty)
      val ss = Await.result(dao.saveSeason(Season(0L, 2017,"", None)), testDbTimeout)
      val ts = Await.result(dao.saveTeams(List(
        Team(0L, "georgetown","Georgetown","Geogetown","Hoyas","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me"),
        Team(0L, "st-johns","St. John's","St. John's","Red Storm","Big East",None,None,None,None,None,None,None, LocalDateTime.now(),"me")
      )), testDbTimeout)
      private val date =  LocalDate.parse("2017-01-01")
      val gId = Await.result(dao.saveGame(Game(0L, ss.id,ts(0).id,ts(1).id,date, date.atStartOfDay(),None, false,None,None,None,"test",LocalDateTime.now(),"me"),None),testDbTimeout)

      val r = Await.result(dao.saveResult(Result(0L,gId,100,95,2,LocalDateTime.now(),"me")), testDbTimeout)
      assert(r.id>0)

     try {
       val q = Await.result(dao.saveResult(Result(0L,gId,105,95,2,LocalDateTime.now(),"me")), testDbTimeout)
       fail("Game can only have one result")
     } catch {
       case e:Exception=> //OK
     }

    }

  }


}
