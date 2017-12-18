package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LogisticRegressionContext {

  def createClassifier(fm: FeatureMapper[(Game, Option[Result])], cat: Categorizer[(Game, Option[Result])], ss: List[Int], xm: List[Int], dao: ScheduleDAO): Future[LogisticRegressionContext] = {
    selectTrainingSet(ss, xm, dao).map(games => {
      create(fm, cat, games, dao)
    })
  }

  def create(featureMapper: FeatureMapper[(Game, Option[Result])], categorizer: Categorizer[(Game, Option[Result])], games: List[(Game, Option[Result])], dao: ScheduleDAO): LogisticRegressionContext = {
    LogisticRegressionContext("FIXME", LogisticReg.createClassifier(featureMapper, categorizer, games), dao)
  }

  def selectTrainingSet(seasons: List[Int], excludeMonths: List[Int], dao: ScheduleDAO): Future[List[(Game, Option[Result])]] = {
    Future.sequence(
      seasons.map(y => dao.loadSchedule(y))).map(_.flatten.toList).map(_.flatMap(_.gameResults).filter(tup => !excludeMonths.contains(tup._1.date.getMonth.getValue))
    )
  }
}

case class LogisticRegressionContext(key: String, classifier: Classifier[(Game, Option[Result])], dao: ScheduleDAO) extends SchedulePredictor {

  def modelPerformance(games: List[(Game, Option[Result])]): List[LogisticResultLine] = {
    games.flatMap(gor => gor match {
      case (g, Some(result)) =>
        classifier.classify(gor) match {
          case Some(arr) =>
            Some(LogisticResultLine(g, result.homeScore, result.awayScore, arr(0), arr(1)))
          case None => None
        }
      case (g, None) => None
    })
  }

  override def predictDate(sch: Schedule, d: LocalDate): List[GamePrediction] = {
    val gs = gamesForDate(sch, d)
    logger.info(s"Predicting ${gs.size} games for date $d`")
    gs.flatMap(classifyGames)
  }

  override def predictTeam(sch: Schedule, k: String): List[GamePrediction] = {
    val gs = gamesForTeam(sch, k)
    logger.info(s"Predicting ${gs.size} games for team $k`")
    gs.flatMap(classifyGames)
  }


  private def classifyGames(gg: (Game, Option[Result])): Option[GamePrediction] = {
    val op = classifier.classify(gg)
    op.map(p => {
      if (p(0) > 0.5) {
        GamePrediction(0L, gg._1.id, key, Some(gg._1.homeTeamId), Some(p(0)), None, None)
      } else {
        GamePrediction(0L, gg._1.id, key, Some(gg._1.awayTeamId), Some(1.0 - p(0)), None, None)
      }
    })
  }
}
