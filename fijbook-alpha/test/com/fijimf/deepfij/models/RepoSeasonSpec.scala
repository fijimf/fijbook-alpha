package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class RepoSeasonSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }

  "Seasons " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listSeasons, Duration.Inf).isEmpty)
    }

    "be able to be saved" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), Duration.Inf)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
    }

    "not be able to save the same year" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), Duration.Inf)
      val t = Season(0L, 2017, "", None)
      try {
        Await.result(dao.saveSeason(t), Duration.Inf)
        fail
      } catch {
        case _: Throwable => assert(Await.result(dao.listSeasons, Duration.Inf).size == 1)
      }
    }
    "find a season by id" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), Duration.Inf)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), Duration.Inf)
      val m =Await.result(dao.findSeasonById(k.id), Duration.Inf)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonById(-999), Duration.Inf).isEmpty)
    }

    "find a season by year" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), Duration.Inf)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), Duration.Inf)
      val m =Await.result(dao.findSeasonByYear(2017), Duration.Inf)
      assert(m.isDefined)
      assert(m.get.id == k.id)
      assert(m.get.year == 2017)

      assert(Await.result(dao.findSeasonByYear(2025), Duration.Inf).isEmpty)
    }

    "delete a season" in new WithApplication(FakeApplication()) {
      Await.result(dao.saveSeason(Season(0L, 2016, "", None)), Duration.Inf)
      val k = Await.result(dao.saveSeason(Season(0L, 2017, "", None)), Duration.Inf)
      Await.result(dao.saveSeason(Season(0L, 2018, "", None)), Duration.Inf)
      assert(Await.result(dao.listSeasons, Duration.Inf).size == 3)

      assert(Await.result(dao.deleteSeason(k.id), Duration.Inf)==1)

      assert(Await.result(dao.listSeasons, Duration.Inf).size == 2)

    }

   "lock a season for editing" in new WithApplication(FakeApplication()) {
      val s = Season(0L, 2017, "", None)
      val ss = Await.result(dao.saveSeason(s), Duration.Inf)

     assert(dao.checkAndSetLock(ss.id))
     assert(!dao.checkAndSetLock(ss.id))
    }

   "unlock a season for editing" in new WithApplication(FakeApplication()) {
     val s = Season(0L, 2017, "", None)
     val ss = Await.result(dao.saveSeason(s), Duration.Inf)

     assert(dao.checkAndSetLock(ss.id))
     assert(Await.result(dao.unlockSeason(ss.id),Duration.Inf)>0)
     assert(dao.checkAndSetLock(ss.id))
    }




  }

}
