package com.fijimf.deepfij.models.nstats.predictors

import java.io.File
import java.nio.file.Files

import com.fijimf.deepfij.models.XPredictionModel
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.apache.commons.io.FileUtils
import smile.classification.LogisticRegression

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Predictor[+M <: java.io.Serializable](xm: XPredictionModel, me: ModelEngine[M]) {

  def isTrained: Boolean = me.kernel.isDefined

  def train(dao: ScheduleDAO): Future[Predictor[M]] = if (isTrained) {
    throw new IllegalStateException("Model is already trained. Call retrain to train a new version")
  } else {
    for {
      ss <- dao.loadSchedules()
      me2 <- me.train(ss, dao)
      xp2 <- dao.saveXPredictionModel(xm.copy(id = 0L, version = xm.version + 1))
    } yield {
      val pred = Predictor[M](xp2, me2)
      Predictor.save(pred.xm.key,xm.version, pred.me)
      pred
    }
  }

}

object Predictor {
  def load(key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
    key match {
      case "naive-least-squares" => Some(NaiveLeastSquaresPredictor)
      case "baseline-logistic-predictor" =>
        val file = getFileName(key, version)
        if (Files.isReadable(file.toPath)) {
          val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
          Some(BaselineLogisticPredictor(Some(regression)))
        } else {
          Some(BaselineLogisticPredictor(None))
        }
      case _ => None
    }
  }

  def save(key: String, version: Int, me: ModelEngine[java.io.Serializable]): Unit = {
    val file = getFileName(key, version)
    if (Files.isWritable(file.toPath)) {
      me.kernel.foreach(k => smile.write(k, file.getPath))
    }

  }

  private def getFileName(key: String, version: Int) = {
    new File(s"/${FileUtils.getTempDirectoryPath}/$key/$version/model.txt")
  }
}


