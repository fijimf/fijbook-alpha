package com.fijimf.deepfij.models

import java.time.LocalDateTime

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


  }


}


/*
  def listStatValues: Future[List[StatValue]]

  def listLogisticModel: Future[List[LogisticModelParameter]]

  def listGamePrediction: Future[List[GamePrediction]]

  def loadGamePredictions(games: List[Game], modelKey: String): Future[List[GamePrediction]]

  def saveGamePredictions(gps: List[GamePrediction]): Future[List[Int]]

  def deleteStatValues(dates: List[LocalDate], model: List[String]): Future[Unit]

  def saveStatValues(batchSize: Int, dates: List[LocalDate], model: List[String], stats: List[StatValue]): Unit

  def loadStatValues(statKey: String, modelKey: String): Future[List[StatValue]]

  def loadStatValues(modelKey: String): Future[List[StatValue]]

 */