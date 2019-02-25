package com.fijimf.deepfij.models.nstats.predictors

import java.io
import java.time.LocalDateTime

import cats.data.OptionT
import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Game, Schedule, XPrediction, XPredictionModel}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PredictorContext(dao: ScheduleDAO) {
//  val logger = Logger(this.getClass)
//
//  def loadLatestPredictor(key: String): OptionT[Future, Predictor] = {
//    for {
//      model <- dao.loadLatestPredictionModel(key)
//      predictor <- Predictor.realize(model)
//
//    } yield {
//      if (model.isTrained){
//        predictor
//      } else {
//        for {
//          trained<-trainPredictor
//
//        }
//      }
//    }
//  }
//
//  def loadPredictor(key: String, version: Int): OptionT[Future, Predictor[io.Serializable]] = {
//    logger.info(s"Loading latest version of '$key'")
//    for {
//      model <- dao.loadPredictionModel(key, version)
//      predictor <- loadOrTrainKernel(model)
//    } yield {
//      predictor
//    }
//  }
//
//  def loadAllPredictors(key: String): Future[List[Predictor[io.Serializable]]] = {
//    import scala.concurrent.ExecutionContext.Implicits.global
//    (for {
//      models <- OptionT.liftF(dao.loadPredictionModels(key))
//      predictors <- models.map(xp => loadOrTrainKernel(xp)).sequence
//    } yield {
//      predictors
//    }).getOrElse(List.empty[Predictor[io.Serializable]])
//  }
//
//  def loadOrTrainKernel(model: XPredictionModel): OptionT[Future, Predictor[io.Serializable]] = {
//    loadKernel(model).orElse(trainKernel(model))
//  }
//
//  private def trainPredictor(predictor: Predictor): OptionT[Future, Predictor] = for {
//    e <- OptionT.fromOption[Future](Predictor.load(cfg, model.key, model.version))
//    tm <- OptionT.liftF(predictor.train( dao))
//  } yield tm
//
//
//  private def loadKernel(model: XPredictionModel): OptionT[Future, Predictor[io.Serializable]] = for {
//    e <- OptionT.fromOption[Future](Predictor.load(cfg, model.key, model.version))
//  } yield Predictor(model, e)
//
//
//  def updatePredictions(key: String, yyyy: Int): Future[List[XPrediction]] = (for {
//    op <- loadLatestPredictor(key)
//    sch <- OptionT(dao.loadSchedule(yyyy))
//    predictions <- calculateAndSavePredictions(op, sch)
//  } yield {
//    predictions
//  }).getOrElse(List.empty[XPrediction])
//
//
//  def updatePredictions(key: String, version: Int, yyyy: Int): Future[List[XPrediction]] = (for {
//    op <- loadPredictor(key, version)
//    sch <- OptionT(dao.loadSchedule(yyyy))
//    predictions <- calculateAndSavePredictions(op, sch)
//  } yield {
//    predictions
//  }).getOrElse(List.empty[XPrediction])
//
//
//  private def calculateAndSavePredictions(predictor: Predictor[_], s: Schedule): OptionT[Future, List[XPrediction]] = {
//    val hash = ScheduleSerializer.md5Hash(s)
//    val modelId = predictor.xm.id
//    val pp: List[Game] => Future[List[XPrediction]] = predictor.me.predict(s, dao)
//    OptionT.liftF(pp(s.games).flatMap(lst => {
//      dao.updatePredictions(modelId, hash, lst)
//    }))
//  }
}
