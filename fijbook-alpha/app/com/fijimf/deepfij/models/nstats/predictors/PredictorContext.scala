package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Schedule, XPrediction, XPredictionModel}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
case class PredictorContext(dao: ScheduleDAO) {
  val logger=Logger(this.getClass)
  /*
  loadLatest will first load the PredictionModel
  - if there is no prediction model in the database, the Predictor object will
    + create a baseline empty model
    + train the new model
    + save and reload the model
  - If there is a prediction model in the database, the Predictor object will
    + attempt to read in the trained model.  If the trained model fails to load it will
       * train the model
       * save and reload the model
   */
  def loadLatest(key: String): Future[Option[Predictor[_]]] = {
    logger.info(s"Loading lastest versio of '$key'")
    dao.loadLatestPredictionModel(key).flatMap(model => {
      (for {
        m <- model.orElse(Some(XPredictionModel(0L,key,0)))
        e <- Predictor.load(m.key, m.version)
      } yield {
        if (e.kernel.isDefined) {
          Future.successful {
            Predictor(m, e)
          }
        } else {
          Predictor(m, e).train(dao)
        }
      }) match {
        case Some(f) => f.map(Some(_))
        case None =>
          logger.info(s"Failed to load model '$key'")
          Future.successful(None)
      }
    })
  }

  def updatePredictions(key:String, yyyy:Int): Future[List[XPrediction]] = {
    for {
      op<-loadLatest(key)
      sch<-dao.loadSchedule(yyyy)
      predictions<-calculateAndSavePredictions( op, sch)
    } yield{
      predictions
    }
  }

  private def calculateAndSavePredictions(op: Option[Predictor[_]], sch: Option[Schedule]): Future[List[XPrediction]] = {
    (for {
      predictor <- op
      s <- sch
    } yield {
      val hash = ScheduleSerializer.md5Hash(s)
      val modelId = predictor.xm.id
      val pp = predictor.me.predict(s, dao)
      val flist: Future[List[XPrediction]] = Future.sequence(s.incompleteGames.map(pp(_))).map(_.flatten)
      flist.flatMap(lst => {
        dao.updatePredictions(modelId, hash, lst.map(_.copy(modelId = modelId, schedMD5Hash = hash)))
      })
    }).getOrElse(Future.successful(List.empty[XPrediction]))
  }
}
