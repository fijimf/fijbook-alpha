package com.fijimf.deepfij.models

import com.fijimf.deepfij.models.TeamRepo
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play._
import play.api.test.Helpers._
import play.api.test._
import testhelpers.{EvolutionHelper, Injector}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class TeamSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach {

  val teamRepo = Injector.inject[TeamRepo]
  override def afterEach() = EvolutionHelper.clean()

  "Teams " should {
    "be empty initially" in new WithApplication(FakeApplication()) {
      Await.result(teamRepo.all, Duration.Inf) mustBe List.empty
    }




  }

}
