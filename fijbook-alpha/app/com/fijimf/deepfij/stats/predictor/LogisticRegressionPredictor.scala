package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

case class LogisticRegressionPredictor(logisticModelName: String, dao: ScheduleDAO) extends SchedulePredictor {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val modelParameters: Map[String,LogisticModelParameter] =
    Await.result(dao.findLatestLogisticModel(logisticModelName).map(_.map(l=>l.featureName->l).toMap),Duration.Inf)
  override val key = logisticModelName
  private val maxDate: LocalDate = modelParameters.values.map(_.fittedAsOf).maxBy(_.toEpochDay)
  private val featureMapper = StatValueGameFeatureMapper(maxDate, (modelParameters.keySet-"Intercept").toList, dao)

  override def predictDate(gs: List[Game], d: LocalDate, sch: Schedule): List[GamePrediction] = {
    logger.info(s"Generating predictions for $d.  Have ${gs.size} games")
//    gs.flatMap(g => {
//      featureMapper.featureMap((g, None)).map(vs =>  => if (p > 0.5) {
//        GamePrediction(0L, g.id, key, Some(g.homeTeamId), Some(p), None, None)
//      } else {
//        GamePrediction(0L, g.id, key, Some(g.awayTeamId), Some(1.0 - p), None, None)
//      }
//      )
//    })
    List.empty[GamePrediction]
  }
}
