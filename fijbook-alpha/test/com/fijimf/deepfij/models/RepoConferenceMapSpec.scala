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


class RepoConferenceMapSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin  with ScalaFutures {
  implicit override val patienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))
  val dao = Injector.inject[ScheduleDAO]

  "ConferenceMaps " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      assert(Await.result(dao.listConferenceMaps, testDbTimeout).isEmpty)
    }

  }


}
