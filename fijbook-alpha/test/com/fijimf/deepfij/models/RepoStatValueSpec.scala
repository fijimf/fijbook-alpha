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
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

class RepoStatValueSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]


  "StatValues " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listStatValues, 10 seconds).isEmpty)
    }

    "be able to save baby statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(2).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(35).flatMap(t => dates.map(d => StatValue(0L, "model", "stat", t.toLong, d, 0.123))).toList
      Await.result(dao.saveStatValues(15, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 2 * 35)
    }
    "be able to save large statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(15, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
    }

    "be able to save large statValues with a different batch size" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
    }

    "be able to save empty list of statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List.empty[String].map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).isEmpty)
    }

    "be able to update large statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues1 = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues1), 10 seconds)
      private val vals1 = Await.result(dao.listStatValues, 10 seconds)
      assert(vals1.size == 60 * 350 * 3)
      assert(vals1.forall(_.value > 0.0))


      private val statValues2 = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, -9.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues2), 10 seconds)
      private val vals2 = Await.result(dao.listStatValues, 10 seconds)
      assert(vals2.size == 60 * 350 * 3)
      assert(vals2.forall(_.value < 0.0))
    }


    "be able to delete statValues " in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now()), List("xxx-model")), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(61)), List("model")), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(1)), List("model")), 10 seconds)
      private val result = Await.result(dao.listStatValues, 10 seconds)
      assert(result.size == 59 * 350 * 3)
    }

    "be able to load statValues from model" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model"), 10 seconds)
      assert(result.size == 60 * 350 * 3)
    }

    "be able to load statValues from model and stat key" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("Stat3", "model"), 10 seconds)
      assert(result.size == 60 * 350)
    }

    "not be able to load missing statValues from model " in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model-xxx"), 10 seconds)
      assert(result.isEmpty)
    }
    "not be able to load missing statValues from model and stat key " in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d => LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t => dates.flatMap(d => List("Stat1", "Stat2", "Stat3").map(st => StatValue(0L, "model", st, t.toLong, d, 0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), 10 seconds)
      assert(Await.result(dao.listStatValues, 10 seconds).size == 60 * 350 * 3)
      private val result = Await.result(dao.loadStatValues("model", "Stat-999"), 10 seconds)
      assert(result.isEmpty)
    }


  }


}
