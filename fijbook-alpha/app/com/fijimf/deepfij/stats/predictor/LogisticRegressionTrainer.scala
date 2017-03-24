package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models._
import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import org.apache.mahout.math.Matrix

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

case class LogisticRegressionTrainer(date:LocalDate, logisticModelName: String, stats: List[String], dao: ScheduleDAO) extends SchedulePredictor {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val key = logisticModelName

  private val featureMapper = StatValueGameFeatureMapper(date, stats, dao)
  private val logisticReg = LogisticReg(featureMapper, StraightWinCategorizer)

  def trainModel:Future[List[LogisticModelParameter]] = {
    dao.loadLatestSchedule().map(trainOnSched).andThen{
      case Success(lmps)=>
        lmps.map(lmp=>dao.saveLogisticModelParameter(lmp))
      case Failure(ex) =>
        logger.error("Exception training logisitic model on schedule")
        List.empty[Future[LogisticModelParameter]]
    }
  }

  private def trainOnSched(os: Option[Schedule]): List[LogisticModelParameter] = os match {
    case Some(sched) =>
      val model = logisticReg.regress(sched.gameResults)
      val beta: Matrix = logisticReg.logisticRegression.getBeta
      0.until(featureMapper.featureDimension).map { i =>
        val b: Double = beta.get(0, i)
        val keyName = featureMapper.keys(i)
        val(shift, scale)= featureMapper.norms(keyName)
        LogisticModelParameter(0L, logisticModelName, keyName, shift, scale, b, date)
      }.toList
    case None => List.empty[LogisticModelParameter]
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
