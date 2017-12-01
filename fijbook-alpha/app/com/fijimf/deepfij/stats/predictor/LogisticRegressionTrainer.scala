package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LogisticRegressionTrainer {

  def create(featureMapper: FeatureMapper[(Game, Option[Result])], categorizer: Categorizer[(Game, Option[Result])], seasons: List[Int], dao: ScheduleDAO): Future[LogisticRegressionTrainer] = {
    createClassifier(featureMapper, categorizer, seasons, dao).map(c => LogisticRegressionTrainer("FIXME", c, dao))
  }

  def createClassifier(featureMapper: FeatureMapper[(Game, Option[Result])], categorizer: Categorizer[(Game, Option[Result])], seasons: List[Int], dao: ScheduleDAO): Future[Classifier[(Game, Option[Result])]] = {
    Future.sequence(
      seasons.map(y => dao.loadSchedule(y))).map(_.flatten.toList).map(_.flatMap(_.gameResults)
    ).map(games => {
      LogisticReg.createClassifier(featureMapper, categorizer, games)
    })
  }
}

case class LogisticRegressionTrainer(key: String, classifier: Classifier[(Game, Option[Result])], dao: ScheduleDAO) extends SchedulePredictor {
  override def predictDate(gs: List[Game], d: LocalDate, sch: Schedule): List[GamePrediction] = {
    logger.info(s"Generating predictions for $d.  Have ${gs.size} games")
    gs.map((_, Option.empty[Result])).flatMap(gg => {
      val op = classifier.classify(gg)
      op.map(p => {
        if (p(0) > 0.5) {
          GamePrediction(0L, gg._1.id, key, Some(gg._1.homeTeamId), Some(p(0)), None, None)
        } else {
          GamePrediction(0L, gg._1.id, key, Some(gg._1.awayTeamId), Some(1.0 - p(0)), None, None)
        }
      })
    })
  }

}
