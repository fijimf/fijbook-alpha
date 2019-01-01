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


  val cfg: Configuration = Injector.inject[Configuration]
  "Predictor " should {
    "create a filename specific to app" in new WithApplication() {
      val file: File = Predictor.getFileName(cfg, "abcdefg", 99)
      assert(file.getPath === "/tmp/abcdefg/99/model.txt")
    }

    "save a model engine with a serializable kernel" in new WithApplication() {
      val modelName: String = UUID.randomUUID().toString
      Predictor.save(cfg, modelName, 1, DummyModelEngine(Some(s"UNIQUEID-$modelName")))

      private val path: Path = Paths.get(Predictor.getFileName(cfg, modelName, 1).getPath)
      assert(Files.exists(path))
    }

    "load a model engine with a serializable kernel" in new WithApplication() {
      val modelName: String = UUID.randomUUID().toString
      Predictor.save(cfg, modelName, 1, DummyModelEngine(Some(s"UNIQUEID-$modelName")))

      private val path: Path = Paths.get(Predictor.getFileName(cfg, modelName, 1).getPath)
      assert(Files.exists(path))

      private val ome: Option[ModelEngine[_]] = Predictor.load(cfg, modelName, 1)
      assert(ome.isDefined)
      ome.flatMap(_.kernel) match {
        case Some(str) =>
          logger.info(s"Kernel was $str")
          assert(str === s"UNIQUEID-$modelName")
        case _ => fail("Failed to rehydrate save regression")
      }
    }

  }
}
