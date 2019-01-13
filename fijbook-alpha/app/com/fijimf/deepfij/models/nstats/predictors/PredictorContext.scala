package com.fijimf.deepfij.models.nstats.predictors

import cats.implicits._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.services.ScheduleSerializer
import com.fijimf.deepfij.models.{Schedule, XPrediction, XPredictionModel}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class PredictorContext(cfg:Configuration, dao: ScheduleDAO) {
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
    logger.info(s"Loading latest version of '$key'")
    dao.loadLatestPredictionModel(key).flatMap(model => {
      (for {
        m <- model.orElse(Some(XPredictionModel(0L,key,0)))
        e <- Predictor.load(cfg, m.key, m.version)
      } yield {
        if (e.kernel.isDefined) {
          logger.info(s"For ${m.key} version ${m.version} loaded trained kernel.")
          if (m.id === 0) {
            dao.savePredictionModel(m.copy(version = m.version + 1)).map(Predictor(_,e))
          } else {
            Future.successful {
              Predictor(m, e)
            }
          }
        } else {
          Predictor(m, e).train(cfg, dao)
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
      val flist: Future[List[XPrediction]] = pp(s.incompleteGames).map(_.flatten)
      flist.flatMap(lst => {
        dao.updatePredictions(modelId, hash, lst.map(_.copy(modelId = modelId, schedMD5Hash = hash)))
      })
    }).getOrElse(Future.successful(List.empty[XPrediction]))
  }
}
