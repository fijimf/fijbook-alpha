package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._

class RepoSeasonSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach  with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listSeasons, 10 seconds).isEmpty)
    }

    "be able to be saved" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), 10 seconds)
      assert(Await.result(dao.listSeasons, 10 seconds).size == 1)
    }

    "not be able to save the same year" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), 10 seconds)
      val t = Season(0L, 2017, "", None)
      try {
        Await.result(dao.saveSeason(t), 10 seconds)
        fail
      } catch {
        case _: Throwable => assert(Await.result(dao.listSeasons, 10 seconds).size == 1)
      }
    }
    "find a season by id" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), 10 seconds)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), 10 seconds)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), 10 seconds)
      val m =Await.result(dao.findSeasonById(k.id), 10 seconds)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonById(-999), 10 seconds).isEmpty)
    }

    "find a season by year" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), 10 seconds)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), 10 seconds)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), 10 seconds)
      val m =Await.result(dao.findSeasonByYear(2017), 10 seconds)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonByYear(2025), 10 seconds).isEmpty)
    }

    "delete a season" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), 10 seconds)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), 10 seconds)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), 10 seconds)
      assert(Await.result(dao.listSeasons, 10 seconds).size == 3)

      assert(Await.result(dao.deleteSeason(k.id), 10 seconds)==1)

      assert(Await.result(dao.listSeasons, 10 seconds).size == 2)

    }

   "lock a season for editing" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), 10 seconds)

     assert(dao.checkAndSetLock(ss.id))
     assert(!dao.checkAndSetLock(ss.id))
    }

   "unlock a season for editing" in new WithApplication(FakeApplication()) {
     val s = Season(0L, 2017, "", None)
     val ss = Await.result(dao.saveSeason(s), 10 seconds)

     assert(dao.checkAndSetLock(ss.id))
     assert(Await.result(dao.unlockSeason(ss.id),10 seconds)>0)
     assert(dao.checkAndSetLock(ss.id))
    }




  }

}
