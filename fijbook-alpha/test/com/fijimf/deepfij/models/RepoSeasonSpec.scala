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
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listSeasons, testDbTimeout).isEmpty)
    }

    "be able to be saved" in new WithApplication() {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), testDbTimeout)
      assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
    }

    "not be able to save the same year" in new WithApplication() {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), testDbTimeout)
      val t = Season(0L, 2017, "", None)
      try {
        Await.result(dao.saveSeason(t), testDbTimeout)
        fail
      } catch {
        case _: Throwable => assert(Await.result(dao.listSeasons, testDbTimeout).size == 1)
      }
    }
    "find a season by id" in new WithApplication() {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), testDbTimeout)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), testDbTimeout)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), testDbTimeout)
      val m =Await.result(dao.findSeasonById(k.id), testDbTimeout)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonById(-999), testDbTimeout).isEmpty)
    }

    "find a season by year" in new WithApplication() {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), testDbTimeout)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), testDbTimeout)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), testDbTimeout)
      val m =Await.result(dao.findSeasonByYear(2017), testDbTimeout)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonByYear(2025), testDbTimeout).isEmpty)
    }

    "delete a season" in new WithApplication() {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), testDbTimeout)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), testDbTimeout)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), testDbTimeout)
      assert(Await.result(dao.listSeasons, testDbTimeout).size == 3)

      assert(Await.result(dao.deleteSeason(k.id), testDbTimeout)==1)

      assert(Await.result(dao.listSeasons, testDbTimeout).size == 2)

    }

   "lock a season for editing" in new WithApplication() {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), testDbTimeout)

     assert(dao.checkAndSetLock(ss.id))
     assert(!dao.checkAndSetLock(ss.id))
    }

   "unlock a season for editing" in new WithApplication() {
     val s = Season(0L, 2017, "", None)
     val ss = Await.result(dao.saveSeason(s), testDbTimeout)

     assert(dao.checkAndSetLock(ss.id))
     assert(Await.result(dao.unlockSeason(ss.id),testDbTimeout)>0)
     assert(dao.checkAndSetLock(ss.id))
    }




  }

}
