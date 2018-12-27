package com.fijimf.deepfij.models.nstats.predictors

import java.io.File
import java.nio.file.Files

import com.fijimf.deepfij.models.XPredictionModel
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.apache.commons.io.FileUtils
import play.api.Logger
import smile.classification.LogisticRegression

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class Predictor[+M <: java.io.Serializable](xm: XPredictionModel, me: ModelEngine[M]) {
val logger = Logger(this.getClass)
  def isTrained: Boolean = me.kernel.isDefined

  def train(dao: ScheduleDAO): Future[Predictor[M]] = if (isTrained) {
    throw new IllegalStateException("Model is already trained. Call retrain to train a new version")
  } else {
    logger.info(s"Training model for ${xm.key}\\${xm.version}")
    for {
      ss <- dao.loadSchedules()
      me2 <- me.train(ss, dao)
      xp2 <- dao.savePredictionModel(xm.copy(id = 0L, version = xm.version + 1))
    } yield {
      val pred = Predictor[M](xp2, me2)
      Predictor.save(pred.xm.key,xm.version, pred.me)
      pred
    }
  }

}

object Predictor {
  val logger = Logger(this.getClass)
  def load(key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    key match {
      case "naive-least-squares" => Some(NaiveLeastSquaresPredictor)
      case "baseline-logistic-predictor" =>
        val file = getFileName(key, version)
        logger.info(s"Trying to read trained model from ${file.toString}")
        if (Files.isReadable(file.toPath)) {
          val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
          logger.info(s"Read model from file ${file.toString}")
          Some(BaselineLogisticPredictor(Some(regression)))
        } else {
          logger.info(s"Could not read model from file ${file.toString}")
          Some(BaselineLogisticPredictor(None))
        }
      case _ => None
    }
  }

  def save(key: String, version: Int, me: ModelEngine[java.io.Serializable]): Unit = {
    Try {
      val file = getFileName(key, version)
      logger.info(s"Writing trained model to ${file.toString}")
      if (Files.isWritable(file.toPath)) {
        me.kernel.foreach(k => smile.write(k, file.getPath))
      }
    } match {
      case Success(_)=>logger.info("Successfully wrote model")
      case Failure(ex)=>logger.info("Failed to write model.  Exception was "+ex)
    }

  }

  private def getFileName(key: String, version: Int): File = {
    new File(s"/${FileUtils.getUserDirectoryPath}/$key/$version/model.txt")
  }
}


