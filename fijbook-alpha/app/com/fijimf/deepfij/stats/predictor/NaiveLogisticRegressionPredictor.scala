package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class NaiveLogisticRegressionPredictor(statKey: String, modelKey: String, dao: ScheduleDAO) extends SchedulePredictor {


  import scala.concurrent.ExecutionContext.Implicits.global

  override val key = "logistic-regression" + statKey

  private val featureMapper = StatValueGameFeatureMapper(statKey, modelKey, dao)
  private val logisticReg = LogisticReg(featureMapper, StraightWinCategorizer)

  private val model: Option[List[Double]] = Await.result(dao.loadLatestSchedule().map(trainOnSched), Duration.Inf)

  logger.info(s"For predictor $key model is "+model)
  private def trainOnSched(os: Option[Schedule]): Option[List[Double]] = os match {
    case Some(sched) => Some(logisticReg.regress(sched.gameResults))
    case None => None
  }


  override def predictDate(gs: List[Game], d: LocalDate, sch: Schedule): List[GamePrediction] = {
    logger.info(s"Generating predictions for $d.  Have ${gs.size} games")
    gs.flatMap(g => {
      featureMapper.feature((g, None)).map(v => logisticReg.classify(v)).map(p => if (p > 0.5) {
        GamePrediction(0L, g.id, key, Some(g.homeTeamId), Some(p), None, None)
      } else {
        GamePrediction(0L, g.id, key, Some(g.awayTeamId), Some(1.0 - p), None, None)
      }
      )
    })
  }


}
