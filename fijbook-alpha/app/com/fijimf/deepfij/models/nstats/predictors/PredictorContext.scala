package com.fijimf.deepfij.models.nstats.predictors

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
case class PredictorContext(dao: ScheduleDAO) {
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
    dao.loadLatestPredictionModel(key).flatMap(model => {
      (for {
        m <- model
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
        case None => Future.successful(None)
      }
    })
  }
}
