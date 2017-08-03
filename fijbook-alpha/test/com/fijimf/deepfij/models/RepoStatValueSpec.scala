package com.fijimf.deepfij.models

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await
import scala.concurrent.duration._

class RepoStatValueSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin {
  val dao = Injector.inject[ScheduleDAO]

  val nextSixtyDays = {
    1.to(60).map(d => LocalDate.now().plusDays(d)).toList
  }
  
  "StatValues " should {
    "be empty initially" in new WithApplication() {
      assertStatValuesIsEmpty()
    }

    "be able to save baby statValues" in new WithApplication() {
      assertStatValuesIsEmpty()
      val dates = 1.to(2).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(35).flatMap(t => dates.map(d => StatValue(0L, "model", "stat", t.toLong, d, 0.123))).toList
      Await.result(dao.saveStatValues( dates, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 2 * 35)
    }
    "be able to save large statValues" in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)
    }

    "be able to save empty list of statValues" in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List.empty[String].map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assertStatValuesIsEmpty()
    }

    "be able to update large statValues" in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues1 = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues1), testDbTimeout)
      private val vals1 = Await.result(dao.listStatValues, testDbTimeout)
      assert(vals1.size == 60 * 350 * 3)
      assert(vals1.forall(_.value > 0.0))


      private val statValues2 = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, -9.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues2), testDbTimeout)
      private val vals2 = Await.result(dao.listStatValues, testDbTimeout)
      assert(vals2.size == 60 * 350 * 3)
      assert(vals2.forall(_.value < 0.0))
    }


    "be able to delete statValues " in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now()), List("xxx-model")), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(61)), List("model")), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(1)), List("model")), testDbTimeout)
      private val result = Await.result(dao.listStatValues, testDbTimeout)
      assert(result.size == 59 * 350 * 3)
    }

    "be able to load statValues from model" in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model"), testDbTimeout)
      assert(result.size == 60 * 350 * 3)
    }

    "be able to load statValues from model and stat key" in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("Stat3", "model"), testDbTimeout)
      assert(result.size == 60 * 350)
    }

    "not be able to load missing statValues from model " in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model-xxx"), testDbTimeout)
      assert(result.isEmpty)
    }
    "not be able to load missing statValues from model and stat key " in new WithApplication() {
      assertStatValuesIsEmpty()
      private val statValues = 1.to(350).flatMap(t => nextSixtyDays.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues( nextSixtyDays, List("model"), statValues), testDbTimeout)
      assert(Await.result(dao.listStatValues, testDbTimeout).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model", "Stat-999"), testDbTimeout)
      assert(result.isEmpty)
    }
  }

  private def assertStatValuesIsEmpty() = {
    assert(Await.result(dao.listStatValues, testDbTimeout).isEmpty)
  }
}
