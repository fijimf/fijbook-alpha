package com.fijimf.deepfij.models

import java.sql.SQLException
import java.time.LocalDateTime

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test._
import testhelpers.Injector

import scala.concurrent.Await


class RepoPredictionSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {
  val dao: ScheduleDAO = Injector.inject[ScheduleDAO]

  "PredictionModels " should {

    "be able to be saved" in new WithApplication() {
      val m: XPredictionModel = XPredictionModel(0L, "test-model", 1, Some("test"), LocalDateTime.now())
      val n: Option[XPredictionModel] = Await.result(dao.savePredictionModel(m).value, testDbTimeout)

      n match {
        case Some(xm)=>
          assert(xm.id > 0)
          assert(xm.key === "test-model")
          assert(xm.version === 1)
        case None=>fail("Save failed")
      }
    }

    "not save an untrained model" in new WithApplication() {
      val m1 = XPredictionModel("test-model", 1)

      try {
        Await.result(dao.savePredictionModel(m1).value, testDbTimeout)
        fail("Save of untrained model did not throw an exception")
      } catch {
        case _: IllegalArgumentException => //OK
      }
    }

    "be unique by key and version" in new WithApplication() {
      val m1 = XPredictionModel("test-model", 1, "test")
      val m2 = XPredictionModel("test-model", 1, "test")
      val n: Option[XPredictionModel] = Await.result(dao.savePredictionModel(m1).value, testDbTimeout)

      try {
        Await.result(dao.savePredictionModel(m2).value, testDbTimeout)
        fail("Non-unique save did not throw an exception")
      } catch {
        case _: SQLException => //OK
      }
    }

    "ensure that key is not blank and version is > 0" in new WithApplication() {
      try {
        Await.result(dao.savePredictionModel(XPredictionModel("", 1)).value, testDbTimeout)
        fail("Blank key did not throw exception")
      } catch {
        case _: IllegalArgumentException => //OK
      }


      try {
        Await.result(dao.savePredictionModel(XPredictionModel( "test-model", 0)).value, testDbTimeout)
        fail("Zero version did not throw exception")
      } catch {
        case _: IllegalArgumentException => //OK
      }
    }


    "be able to retrieve the latest version" in new WithApplication() {
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 1,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 2,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 3,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 4,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-y-model", 2,"test")).value, testDbTimeout)
      val optX: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-x-model").value, testDbTimeout)
      assert(optX.isDefined)
      assert(optX.exists(_.key === "test-x-model"))
      assert(optX.exists(_.version === 4))
      assert(optX.exists(_.engineData === Some("test")))
      val optY: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-y-model").value, testDbTimeout)
      assert(optY.isDefined)
      assert(optY.exists(_.key === "test-y-model"))
      assert(optY.exists(_.version === 2))
      assert(optY.exists(_.engineData === Some("test")))
    }

    "be able to create a new version if none exists for the key" in new WithApplication() {
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 1,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 2,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 3,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-x-model", 4,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel( "test-y-model", 2,"test")).value, testDbTimeout)
      val optZ: Option[XPredictionModel] = Await.result(dao.loadLatestPredictionModel("test-z-model").value, testDbTimeout)
      assert(optZ.isDefined)
      assert(optZ.exists(_.key === "test-z-model"))
      assert(optZ.exists(_.version === 0))
      assert(optZ.exists(_.engineData === None))
    }

    "be able to retrieve a particular version" in new WithApplication() {
      Await.ready(dao.savePredictionModel(XPredictionModel("test-x-model", 1,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel("test-x-model", 2,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel("test-x-model", 3,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel("test-x-model", 4,"test")).value, testDbTimeout)
      Await.ready(dao.savePredictionModel(XPredictionModel("test-y-model", 2,"test")).value, testDbTimeout)
      val optX: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-x-model", 3).value, testDbTimeout)
      assert(optX.isDefined)
      assert(optX.exists(_.key === "test-x-model"))
      assert(optX.exists(_.version === 3))
      val optY2: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-y-model", 2).value, testDbTimeout)
      assert(optY2.isDefined)
      assert(optY2.exists(_.key === "test-y-model"))
      assert(optY2.exists(_.version === 2))
      val optY3: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-y-model", 3).value, testDbTimeout)
      assert(optY3.isEmpty)

      val optZ: Option[XPredictionModel] = Await.result(dao.loadPredictionModel("test-z-model", 1).value, testDbTimeout)
      assert(optZ.isEmpty)
    }
  }
}
