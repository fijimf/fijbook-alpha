package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.{ScheduleDAO, StatValueDAO}
import com.fijimf.deepfij.models.{Game, Result, Schedule, XPrediction, XPredictionModel, XPredictionModelParameter}

import scala.concurrent.Future


case class Predictor(mr:ModelRecord, me:ModelEngine){

  import scala.concurrent.ExecutionContext.Implicits.global

  def train()(implicit dao:ScheduleDAO): Future[Predictor] ={
    dao.loadSchedules().flatMap(ss=>{
      val parameters = me.train(ss, dao)
      for {
        newXPM <- dao.saveXPredictionModel(XPredictionModel(0L, mr.key, mr.version + 1))
        newParms <- dao.saveXPredictionModelParameters(parameters.map(_.copy(modelId = newXPM.id)))
      } yield {
        copy(mr=ModelRecord(newXPM.id,newXPM.key, newXPM.version, Some(newParms)))
      }
    })
  }
}

case class ModelRecord(  modelId:Long, key:String, version:Int, parameters:Option[List[XPredictionModelParameter]])

trait ModelEngine {
  def train(s:List[Schedule], dx:StatValueDAO):List[XPredictionModelParameter]
  def featureExtractor(s:Schedule, dx:StatValueDAO): Game =>Future[Option[Map[String,Double]]]
  def categoryExtractor(s:Schedule,dx:StatValueDAO ): (Game, Result) =>Future[Option[Double]]
  def predictor(s:Schedule, ss:StatValueDAO): (Game,Long) => Future[Option[XPrediction]]
}

object Predictor {
  def load(key:String):Option[ModelEngine]={
    case "naive-least-squares" => NaiveLeastSquaresPredictor
  }
}

case class FeatureContext(s:Schedule, dao:ScheduleDAO)

case class PredictorContext(dao: ScheduleDAO) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def loadLatest(key: String): Option[Future[Predictor]] = {
    Predictor.load(key).map(eng => {
      for {
        model <- dao.loadLatestPredictionModel(key)
        predictor <- loadPredictor(key, model, eng)
      } yield {
        predictor
      }
    })
  }

  def loadPredictor(key: String, model: Option[XPredictionModel], eng:ModelEngine): Future[Predictor] = {
    model match {
      case Some(pm) =>
        val parms: Future[List[XPredictionModelParameter]] = dao.loadParametersForModel(pm.id)
        parms.map(ps => Predictor(ModelRecord(pm.id,key, pm.version, Some(ps)),eng))
      case None =>
        val pm = ModelRecord(0L, key, 0, Some(List.empty[XPredictionModelParameter]))
        Predictor(pm, eng).train()(dao)
    }
  }
}

/*
val op:Option[Predictor] = Predictor.loadLatest("naive-least-squares")
val op2:Option[Predictor] = Predictor.loadVersion("naive-least-squares", 3)

for {
  p<-op
} yield {
  if (p.isTrained) p else p.train()
}


 */
