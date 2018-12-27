package com.fijimf.deepfij.models

import java.sql.SQLException

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class RepoPredictionSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  "PredictionModels " should {
    "be empty initially" in new WithApplication() {
      assert(Await.result(dao.loadLatestPredictionModel("test-model"), testDbTimeout).isEmpty)
    }

    "be able to be saved" in new WithApplication() {
      val m = XPredictionModel(0L, "test-model", 1)
      val n = Await.result(dao.savePredictionModel(m), testDbTimeout)

      assert(n.id > 0)
      assert(n.key === "test-model")
      assert(n.version === 1)
    }

    "be unique by key and version" in new WithApplication() {
      val m1 = XPredictionModel(0L, "test-model", 1)
      val m2 = XPredictionModel(0L, "test-model", 1)
      val n = Await.result(dao.savePredictionModel(m1), testDbTimeout)

      try {
        Await.result(dao.savePredictionModel(m2), testDbTimeout)
        fail("Non-unique save did not throw an exception")
      } catch {
        case _: SQLException => //OK
      }
    }

    "ensure that key is not blank and version is > 0" in new WithApplication() {
      try {
        Await.result(dao.savePredictionModel(XPredictionModel(0L, "", 1)), testDbTimeout)
        fail("Blank key did not throw exception")
      } catch {
        case _: IllegalArgumentException => //OK
      }


      try {
        Await.result(dao.savePredictionModel(XPredictionModel(0L, "test-model", 0)), testDbTimeout)
        fail("Zero version did not throw exception")
      } catch {
        case _: IllegalArgumentException => //OK
      }
    }


    "be able to retrieve the latest version" in new WithApplication() {
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 1)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 2)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 3)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 4)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-y-model", 2)), testDbTimeout)
      val optX: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-x-model"), testDbTimeout)
      assert(optX.isDefined)
      assert(optX.exists(_.key === "test-x-model"))
      assert(optX.exists(_.version === 4))
      val optY: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-y-model"), testDbTimeout)
      assert(optY.isDefined)
      assert(optY.exists(_.key === "test-y-model"))
      assert(optY.exists(_.version === 2))
      val optZ: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-z-model"), testDbTimeout)
      assert(optZ.isEmpty)
    }

    "be able to retrieve a particular version" in new WithApplication() {
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 1)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 2)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 3)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-x-model", 4)), testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel(0L, "test-y-model", 2)), testDbTimeout)
      val optX: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-x-model", 3), testDbTimeout)
      assert(optX.isDefined)
      assert(optX.exists(_.key === "test-x-model"))
      assert(optX.exists(_.version === 3))
      val optY2: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-y-model", 2), testDbTimeout)
      assert(optY2.isDefined)
      assert(optY2.exists(_.key === "test-y-model"))
      assert(optY2.exists(_.version === 2))
      val optY3: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-y-model", 3), testDbTimeout)
      assert(optY3.isEmpty)

      val optZ: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-z-model", 1), testDbTimeout)
      assert(optZ.isEmpty)
    }
  }
}
