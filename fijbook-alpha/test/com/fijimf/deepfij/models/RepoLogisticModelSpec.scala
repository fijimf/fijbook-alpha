package com.fijimf.deepfij.models

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


class RepoLogisticModelSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]


/*

  def listLogisticModelParameters: Future[List[LogisticModelParameter]]

  def saveLogisticModelParameter(lm: LogisticModelParameter): Future[Int]

  def findLogisticModel(model: String): Future[Map[LocalDate, List[LogisticModelParameter]]]

  def findLogisticModelDate(model: String, asOf: LocalDate): Future[List[LogisticModelParameter]]

  def findLatestLogisticModel(model: String): Future[List[LogisticModelParameter]]

  def deleteLogisticModel(model: String): Future[List[Int]]

  def deleteLogisticModelDate(model: String, asOf: LocalDate): Future[List[Int]]

 */

  import scala.concurrent.ExecutionContext.Implicits.global

  val sampleData = List(
    LogisticModelParameter(0L, "My Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-04")),
    LogisticModelParameter(0L, "My Model", "param1", 0.0, 1.0, 0.1234567, LocalDate.parse("2017-02-05")),
    LogisticModelParameter(0L, "My Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-06")),
    LogisticModelParameter(0L, "My Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-07")),
    LogisticModelParameter(0L, "My Model", "param1", 0.0, 1.0, 0.9999999, LocalDate.parse("2017-02-08")),


    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-04")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-04")),
    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.0123456, LocalDate.parse("2017-02-05")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.1234567, LocalDate.parse("2017-02-05")),
    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-06")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-06")),
    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-07")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-07")),
    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-08")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.3333333, LocalDate.parse("2017-02-08")),
    LogisticModelParameter(0L, "My Other Model", "param1", 0.0, 1.0, 0.8888888, LocalDate.parse("2017-02-09")),
    LogisticModelParameter(0L, "My Other Model", "param2", 0.0, 1.0, 0.9999999, LocalDate.parse("2017-02-09"))
  )
  "LogisticModels " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.listLogisticModelParameters, testDbTimeout).isEmpty)
    }

    "save logistic model parameters" in new WithApplication() {

      Future.sequence(sampleData.map(lm=>dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_)=> {
          val result = Await.result(dao.listLogisticModelParameters, testDbTimeout)
          assert(result.size===17)
        }
      })
    }
    "find all parameters for all dates by model" in new WithApplication() {

      Await.result(Future.sequence(sampleData.map(lm => dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_) => {
          val result = Await.result(dao.listLogisticModelParameters, testDbTimeout)
          assert(result.size === 17)
        }
      }), 30.seconds)

      Await.result(dao.findLogisticModel("My Model").andThen({
        case Success(m) =>
          assert(m.size === 5)
          assert(m.values.forall(_.size===1))
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLogisticModel("My Other Model").andThen({
        case Success(m) =>
          assert(m.size === 6)
          assert(m.values.forall(_.size===2))
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLogisticModel("My Nonexistant Model").andThen({
        case Success(m) =>
          assert(m.size === 0)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
    }
    "find all parameters for by date and model" in new WithApplication() {

      Await.result(Future.sequence(sampleData.map(lm => dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_) => {
          val result = Await.result(dao.listLogisticModelParameters, testDbTimeout)
          assert(result.size === 17)
        }
      }), 30.seconds)

      Await.result(dao.findLogisticModelDate("My Model", LocalDate.parse("2017-02-05")).andThen({
        case Success(m) =>
          assert(m.size === 1)
          assert(m(0).coefficient===0.1234567)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLogisticModelDate("My Other Model", LocalDate.parse("2017-02-05")).andThen({
        case Success(m) =>
          assert(m.size === 2)
          assert(m(0).coefficient===0.1234567 || m(1).coefficient===0.1234567)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLogisticModelDate("My Nonexistant Model", LocalDate.parse("2017-02-05")).andThen({
        case Success(m) =>
          assert(m.size === 0)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
      Await.result(dao.findLogisticModelDate("My Model", LocalDate.parse("2017-01-31")).andThen({
        case Success(m) =>
          assert(m.size === 0)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
    }
   "find latest parameters for by model" in new WithApplication() {

      Await.result(Future.sequence(sampleData.map(lm => dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_) => {
          val result = Await.result(dao.listLogisticModelParameters, testDbTimeout)
          assert(result.size === 17)
        }
      }), 30.seconds)

      Await.result(dao.findLatestLogisticModel("My Model").andThen({
        case Success(m) =>
          assert(m.size === 1)
          assert(m(0).coefficient===0.9999999)
          assert(m(0).fittedAsOf===LocalDate.parse("2017-02-08"))
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLatestLogisticModel("My Other Model").andThen({
        case Success(m) =>
          assert(m.size === 2)
          assert(m(0).coefficient===0.9999999 || m(1).coefficient===0.9999999)
          assert(m(0).fittedAsOf===LocalDate.parse("2017-02-09"))
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
     Await.result(dao.findLatestLogisticModel("My Nonexistant Model").andThen({
        case Success(m) =>
          assert(m.size === 0)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
    }


    "delete all parameters for all dates by model" in new WithApplication() {

      Await.result(Future.sequence(sampleData.map(lm => dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_) => {
          assert(Await.result(dao.listLogisticModelParameters, testDbTimeout).size === 17)
        }
      }), 30.seconds)

      Await.result(dao.deleteLogisticModel("My Other Model").andThen({
        case Success(m) =>
          assert(m === 12)
          assert(Await.result(dao.listLogisticModelParameters, testDbTimeout).size === 5)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
    }
    "delete all parameters by date and model" in new WithApplication() {

      Await.result(Future.sequence(sampleData.map(lm => dao.saveLogisticModelParameter(lm))).andThen({
        case Success(_) => {
          assert(Await.result(dao.listLogisticModelParameters, testDbTimeout).size === 17)
        }
      }), 30.seconds)

      Await.result(dao.deleteLogisticModelDate("My Other Model",LocalDate.parse("2017-02-09")).andThen({
        case Success(m) =>
          assert(m === 2)
          assert(Await.result(dao.listLogisticModelParameters, testDbTimeout).size === 15)
        case Failure(_) => fail("Unexpected exception")
      }), 30.seconds)
    }


  }


}
