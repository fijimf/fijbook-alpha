package com.fijimf.deepfij.models

import java.time.{LocalDate, LocalDateTime}

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class RepoStatValueSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val repo = Injector.inject[ScheduleRepository]
  val dao = Injector.inject[ScheduleDAO]

  override def beforeEach() = {
    Await.result(repo.createSchema(), Duration.Inf)
  }

  override def afterEach() = {
    Await.result(repo.dropSchema(), Duration.Inf)
  }


  "StatValues " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listStatValues, Duration.Inf).isEmpty)
    }

    "be able to save baby statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(2).map(d=> LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(35).flatMap(t=> dates.map(d=>StatValue(0L,"model","stat", t.toLong,d,0.123))).toList
      Await.result(dao.saveStatValues(15, dates, List("model"), statValues), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==2*35)
    }
    "be able to save large statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d=> LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t=> dates.flatMap(d=>List("Stat1", "Stat2", "Stat3").map(st=>StatValue(0L,"model",st, t.toLong,d,0.123)))).toList
      Await.result(dao.saveStatValues(15, dates, List("model"), statValues), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==60*350*3)
    }

    "be able to save large statValues with a different batch size" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d=> LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t=> dates.flatMap(d=>List("Stat1", "Stat2", "Stat3").map(st=>StatValue(0L,"model",st, t.toLong,d,0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==60*350*3)
    }

    "be able to update large statValues" in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d=> LocalDate.now().plusDays(d)).toList
      private val statValues1 = 1.to(350).flatMap(t=> dates.flatMap(d=>List("Stat1", "Stat2", "Stat3").map(st=>StatValue(0L,"model",st, t.toLong,d,0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues1), Duration.Inf)
      private val vals1 = Await.result(dao.listStatValues, Duration.Inf)
      assert(vals1.size==60*350*3)
      assert(vals1.forall(_.value>0.0))


      private val statValues2 = 1.to(350).flatMap(t=> dates.flatMap(d=>List("Stat1", "Stat2", "Stat3").map(st=>StatValue(0L,"model",st, t.toLong,d,-9.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues2), Duration.Inf)
      private val vals2 = Await.result(dao.listStatValues, Duration.Inf)
      assert(vals2.size==60*350*3)
      assert(vals2.forall(_.value<0.0))
    }


    "be able to delete statValues " in new WithApplication(FakeApplication()) {
      val dates = 1.to(60).map(d=> LocalDate.now().plusDays(d)).toList
      private val statValues = 1.to(350).flatMap(t=> dates.flatMap(d=>List("Stat1", "Stat2", "Stat3").map(st=>StatValue(0L,"model",st, t.toLong,d,0.123)))).toList
      Await.result(dao.saveStatValues(5, dates, List("model"), statValues), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==60*350*3)

      Await.result(dao.deleteStatValues(List(LocalDate.now()),List("xxx-model")), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==60*350*3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(61)),List("model")), Duration.Inf)
      assert(Await.result(dao.listStatValues, Duration.Inf).size==60*350*3)

      Await.result(dao.deleteStatValues(List(LocalDate.now().plusDays(1)),List("model")), Duration.Inf)
      private val result = Await.result(dao.listStatValues, Duration.Inf)
      assert(result.size==59*350*3)
    }




  }


}


/*
  def listStatValues: Future[List[StatValue]]

  def deleteStatValues(dates: List[LocalDate], model: List[String]): Future[Unit]

  def saveStatValues(batchSize: Int, dates: List[LocalDate], model: List[String], stats: List[StatValue]): Unit

  def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]]

  def loadStatValues(modelKey: String): Future[List[StatValue]]

 */