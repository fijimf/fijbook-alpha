package com.fijimf.deepfij.models.nstats.predictors

import java.io.File
import java.nio.file.Files

import com.fijimf.deepfij.models.XPredictionModel
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import play.api.{Configuration, Logger}
import smile.classification.LogisticRegression

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

final case class Predictor[+M <: java.io.Serializable](xm: XPredictionModel, me: ModelEngine[M]) {
  val logger = Logger(this.getClass)
  def isTrained: Boolean = me.kernel.isDefined

  def train(cfg: Configuration, dao: ScheduleDAO): Future[Predictor[M]] = if (isTrained) {
    throw new IllegalStateException("Model is already trained. Call retrain to train a new version")
  } else {
    logger.info(s"Training model for ${xm.key}\\${xm.version}")
    for {
      ss <- dao.loadSchedules()
      me2 <- me.train(ss, dao)
      xp2 <- dao.savePredictionModel(xm.copy(id = 0L, version = xm.version + 1)).value
    } yield {
      xp2 match {
        case Some(xx)=>
          val pred = Predictor[M](xx, me2)
          Predictor.save(cfg, pred.xm.key, pred.xm.version, pred.me)
          pred
        case None=>this
      }
    }
  }

}

object Predictor {
  val logger = Logger(this.getClass)

  val predictionModels: Map[String, (Configuration, String, Int) => Option[ModelEngine[java.io.Serializable]]] = Map(
    "naive-least-squares" -> loadNaiveLeastSquaresPredictor,
    "baseline-logistic-predictor" -> loadBaseLineLogisticPredictor,
    "nextgen-logistic-predictor" -> loadNextGenLogisticPredictor
  )

  def load(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    predictionModels.get(key) match {
      case Some(f) => f(cfg, key, version)
      case _ => loadDummyModel(cfg, key, version)
    }
  }

  private def loadNaiveLeastSquaresPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    Some(NaiveLeastSquaresPredictor)
  }

  private def loadDummyModel(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    val file = getFileName(cfg, key, version)

    logger.info(s"Trying to read trained model from ${file.toString}")
    if (Files.isReadable(file.toPath)) {
      val state = smile.read(file.getPath).asInstanceOf[String]
      logger.info(s"Read model from file ${file.toString}")
      Some(DummyModelEngine(Some(state)))
    } else {
      logger.info(s"Could not read model from file ${file.toString}")
      Some(DummyModelEngine())
    }
  }

  private def loadNextGenLogisticPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    val file = getFileName(cfg, key, version)

    logger.info(s"Trying to read trained model from ${file.toString}")
    if (Files.isReadable(file.toPath)) {
      val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
      logger.info(s"Read model from file ${file.toString}")
      Some(NextGenLogisticPredictor(Some(regression)))
    } else {
      logger.info(s"Could not read model from file ${file.toString}")
      Some(NextGenLogisticPredictor(None))
    }
  }

  private def loadBaseLineLogisticPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    val file = getFileName(cfg, key, version)

    logger.info(s"Trying to read trained model from ${file.toString}")
    if (Files.isReadable(file.toPath)) {
      val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
      logger.info(s"Read model from file ${file.toString}")
      Some(BaselineLogisticPredictor(Some(regression)))
    } else {
      logger.info(s"Could not read model from file ${file.toString}")
      Some(BaselineLogisticPredictor(None))
    }
  }

  def save(cfg: Configuration, key: String, version: Int, me: ModelEngine[java.io.Serializable]): Unit = {
    val file = getFileName(cfg, key, version)
    Try {
      Files.createDirectories(file.getParentFile.toPath)
      Files.deleteIfExists(file.toPath)
      me.kernel.foreach(k => {
        logger.info(s"Serializing $k to ${file.getPath}")
        smile.write(k, file.getPath)
      })
    } match {
      case Success(_)=>logger.info("Successfully wrote model")
      case Failure(ex)=>logger.error("Failed to write model.  Exception was "+ex, ex)
    }

  }

  def modelTrainedAt(cfg: Configuration, key: String, version: Int)={
   val file = getFileName(cfg,key,version)
    Files.getLastModifiedTime(file.toPath)
  }

  def getFileName(cfg: Configuration, key: String, version: Int): File = {
    val dir = cfg.get[String]("deepfij.modelDirectory")
    new File(s"/$dir/$key/$version/model.txt")
  }
}


