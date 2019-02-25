package com.fijimf.deepfij.models.nstats.predictors

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

import com.fijimf.deepfij.models.RebuildDatabaseMixin
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import play.api.{Configuration, Logger}
import play.test.WithApplication
import testhelpers.Injector

class PredictorSpec extends PlaySpec with OneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {

  val logger = Logger(this.getClass)
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(250, Millis))

}
