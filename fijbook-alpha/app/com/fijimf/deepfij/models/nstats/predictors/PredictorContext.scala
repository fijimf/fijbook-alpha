package com.fijimf.deepfij.models.nstats.predictors

import java.io

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction, XPredictionModel}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PredictorContext(cfg:Configuration, dao: ScheduleDAO) {
  val logger=Logger(this.getClass)

  def loadLatestPredictor(key: String): Future[Predictor[io.Serializable]] = {
    logger.info(s"Loading latest version of '$key'")
    for {
      model <- dao.loadLatestPredictionModel(key)
      predictor <- loadOrTrainKernel(Some(model))
    } yield {
      predictor
    }
  }

  def loadPredictor(key: String, version: Int): Future[Predictor[io.Serializable]] = {
    logger.info(s"Loading latest version of '$key'")
    for {
      model <- dao.loadPredictionModel(key, version)
      predictor <- loadOrTrainKernel(model)
    } yield {
      predictor
    }
  }

  def loadAllPredictors(key:String):Future[List[Predictor[io.Serializable]]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      models <- dao.loadPredictionModel(key)
      predictors <- Future.sequence(models.map(xp=>loadOrTrainKernel(Some(xp)).collect()))
    } yield {
      predictors
    }
  }

  def loadOrTrainKernel(model: Option[XPredictionModel]): Future[Predictor[io.Serializable]] = {
    loadKernel(model).orElse(trainKernel(model)) match {
      case Some(p)=>p
      case _=> Future.failed(new RuntimeException("Failed to load or train kernel"))
    }
  }

  private def trainKernel(model: Option[XPredictionModel]): Option[Future[Predictor[java.io.Serializable]]] = {
    for {
      m <- model
      e <- Predictor.load(cfg, m.key, m.version)
    } yield {
      Predictor(m, e).train(cfg, dao)
    }
  }

  private def loadKernel(model: Option[XPredictionModel]): Option[Future[Predictor[java.io.Serializable]]] = {
    for {
      m <- model
      e <- Predictor.load(cfg, m.key, m.version)
      x <- e.kernel
    } yield {
      Future.successful(Predictor(m, e))
    }
  }

  def updatePredictions(key:String, yyyy:Int): Future[List[XPrediction]] = {
    for {
      op <- loadLatestPredictor(key)
      sch<-dao.loadSchedule(yyyy)
      predictions<-calculateAndSavePredictions( op, sch)
    } yield{
      predictions
    }
  }

  def updatePredictions(key: String, version: Int, yyyy: Int): Future[List[XPrediction]] = {
    for {
      op <- loadPredictor(key, version)
      sch <- dao.loadSchedule(yyyy)
      predictions <- calculateAndSavePredictions(op, sch)
    } yield {
      predictions
    }
  }

  private def calculateAndSavePredictions(op: Predictor[_], sch: Option[Schedule]): Future[List[XPrediction]] = {
    sch.map(calculateAndSavePredictions(op, _)).getOrElse(Future.successful(List.empty[XPrediction]))
  }

  private def calculateAndSavePredictions(predictor: Predictor[_], s: Schedule) = {
    val hash = ScheduleSerializer.md5Hash(s)
    val modelId = predictor.xm.id
    val pp: List[Game] => Future[List[XPrediction]] = predictor.me.predict(s, dao)
    pp(s.games).flatMap(lst => {
      dao.updatePredictions(modelId, hash, lst)
    })
  }
}
