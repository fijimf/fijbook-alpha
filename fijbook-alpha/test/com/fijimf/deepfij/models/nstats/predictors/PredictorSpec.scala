package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.RebuildDatabaseMixin
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Logger

class PredictorSpec extends PlaySpec with GuiceOneAppPerTest with BeforeAndAfterEach with RebuildDatabaseMixin with ScalaFutures {

  val logger = Logger(this.getClass)

}
