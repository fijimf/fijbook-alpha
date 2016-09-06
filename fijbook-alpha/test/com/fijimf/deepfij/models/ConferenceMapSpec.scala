package com.fijimf.deepfij.models

import java.sql.SQLException

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.test._
import testhelpers.{EvolutionHelper, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class ConferenceMapSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach {

  val repo = Injector.inject[Repo]
  override def afterEach() = EvolutionHelper.clean()

  "ConferenceMaps " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(repo.all(repo.conferenceMaps), Duration.Inf) mustBe List.empty
    }
  }
}
