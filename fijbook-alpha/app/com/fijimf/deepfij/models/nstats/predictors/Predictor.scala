package com.fijimf.deepfij.models.nstats.predictors

import cats.data.OptionT
import com.fijimf.deepfij.models.XPredictionModel
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.Future


// 1) A predictor is the realization of aa algorithm
// 2) A predictionContext is a standardized environment to retrieve features and categories
// 3) With a predictionContext a predictor can be created and trained
// 4) With a predictionContext a predictor can generate predictions
// 5) A trained predictor can be saved
// 6) You should not be able to save an untrained predictor

// This is all very messy
// What do I want to do?
// LoadLatestOrCreateModel
//   LoadLatest
//     --If none create an empty one
// Must have:
//   I want to create & train a new version of a model
//   I want to load

trait Predictor {
//  val model: XPredictionModel
//  val engine: ModelEngine
//
//  def train(dao: ScheduleDAO): OptionT[Future,Predictor] = if (model.isTrained) {
//    throw new IllegalStateException("Model is already trained. Call retrain to train a new version")
//  } else {
//    for {
//      ss <- dao.loadSchedules()
//      me2 <- predictor.train(ss, dao)
//        xp2 <- dao.savePredictionModel(xm.copy(id = 0L, version = xm.version + 1)).value
//      } yield {
//        xp2 match {
//          case Some(xx)=>
//            val pred = Predictor[M](xx, me2)
//            Predictor.save(cfg, pred.xm.key, pred.xm.version, pred.me)
//            pred
//          case None=>this
//        }
//      }
//    }

}

object Predictor {

//  def realize(model: XPredictionModel): Predictor = {
//    null
//  }
}

//  val logger = Logger(this.getClass)
//  def isTrained: Boolean = me.kernel.isDefined
//
//  def train(cfg: Configuration, dao: ScheduleDAO): Future[Predictor[M]] = if (isTrained) {
//    throw new IllegalStateException("Model is already trained. Call retrain to train a new version")
//  } else {
//    logger.info(s"Training model for ${xm.key}\\${xm.version}")
//    for {
//      ss <- dao.loadSchedules()
//      me2 <- me.train(ss, dao)
//      xp2 <- dao.savePredictionModel(xm.copy(id = 0L, version = xm.version + 1)).value
//    } yield {
//      xp2 match {
//        case Some(xx)=>
//          val pred = Predictor[M](xx, me2)
//          Predictor.save(cfg, pred.xm.key, pred.xm.version, pred.me)
//          pred
//        case None=>this
//      }
//    }
//  }
//
//}
//
//object Predictor {
//  val logger = Logger(this.getClass)
//
//  val predictionModels: Map[String, (Configuration, String, Int) => Option[ModelEngine[java.io.Serializable]]] = Map(
//    "naive-least-squares" -> loadNaiveLeastSquaresPredictor,
//    "baseline-logistic-predictor" -> loadBaseLineLogisticPredictor,
//    "nextgen-logistic-predictor" -> loadNextGenLogisticPredictor
//  )
//
//
//
//
//  def load(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
//    predictionModels.get(key) match {
//      case Some(f) => f(cfg, key, version)
//      case _ => loadDummyModel(cfg, key, version)
//    }
//  }
//
//  private def loadNaiveLeastSquaresPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
//    Some(NaiveLeastSquaresPredictor)
//  }
//
//  private def loadDummyModel(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
//    val file = getFileName(cfg, key, version)
//
//    logger.info(s"Trying to read trained model from ${file.toString}")
//    if (Files.isReadable(file.toPath)) {
//      val state = smile.read(file.getPath).asInstanceOf[String]
//      logger.info(s"Read model from file ${file.toString}")
//      Some(DummyModelEngine(Some(state)))
//    } else {
//      logger.info(s"Could not read model from file ${file.toString}")
//      Some(DummyModelEngine())
//    }
//  }
//
//  private def loadNextGenLogisticPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
//    val file = getFileName(cfg, key, version)
//
//
//    logger.info(s"Trying to read trained model from ${file.toString}")
//    if (Files.isReadable(file.toPath)) {
//      val modifiedAt = LocalDateTime.ofInstant(Files.getLastModifiedTime(file.toPath).toInstant, ZoneId.systemDefault())
//      val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
//      logger.info(s"Read model from file ${file.toString}")
//      Some(NextGenLogisticPredictor(Some(regression), modifiedAt))
//    } else {
//      logger.info(s"Could not read model from file ${file.toString}")
//      Some(NextGenLogisticPredictor(None, LocalDateTime.now()))
//    }
//  }
//
//  private def loadBaseLineLogisticPredictor(cfg: Configuration, key: String, version: Int): Option[ModelEngine[java.io.Serializable]] = {
//    val file = getFileName(cfg, key, version)
//
//    logger.info(s"Trying to read trained model from ${file.toString}")
//    if (Files.isReadable(file.toPath)) {
//      val modifiedAt = LocalDateTime.ofInstant(Files.getLastModifiedTime(file.toPath).toInstant, ZoneId.systemDefault())
//      val regression = smile.read(file.getPath).asInstanceOf[LogisticRegression]
//      logger.info(s"Read model from file ${file.toString}")
//      Some(BaselineLogisticPredictor(Some(regression),modifiedAt))
//    } else {
//      logger.info(s"Could not read model from file ${file.toString}")
//      Some(BaselineLogisticPredictor(None, LocalDateTime.now()))
//    }
//  }
//
//  def save(cfg: Configuration, key: String, version: Int, me: ModelEngine[java.io.Serializable]): Unit = {
//    val file = getFileName(cfg, key, version)
//    Try {
//      Files.createDirectories(file.getParentFile.toPath)
//      Files.deleteIfExists(file.toPath)
//      me.kernel.foreach(k => {
//        logger.info(s"Serializing $k to ${file.getPath}")
//        smile.write(k, file.getPath)
//      })
//    } match {
//      case Success(_)=>logger.info("Successfully wrote model")
//      case Failure(ex)=>logger.error("Failed to write model.  Exception was "+ex, ex)
//    }
//
//  }
//
//  def modelTrainedAt(cfg: Configuration, key: String, version: Int)={
//   val file = getFileName(cfg,key,version)
//    Files.getLastModifiedTime(file.toPath)
//  }
//
//  def getFileName(cfg: Configuration, key: String, version: Int): File = {
//    val dir = cfg.get[String]("deepfij.modelDirectory")
//    new File(s"/$dir/$key/$version/model.txt")
//  }
//}


