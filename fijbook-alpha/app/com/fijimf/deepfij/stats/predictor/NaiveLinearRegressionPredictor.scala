package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, GamePrediction, Schedule, ScheduleDAO}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class NaiveLinearRegressionPredictor(dao:ScheduleDAO) extends SchedulePredictor {

  override val key = "naive-regression"

  val values = Await.result(dao.loadStatValues("x-margin-no-ties", "least-squares").map(sv => {
    logger.info(s"Loaded ${sv.size} model values")
    sv.groupBy(_.date).mapValues(_.map(s => s.teamID -> s).toMap)
  }), Duration.Inf)

  override def predictDate(gs:List[Game], d: LocalDate, sch: Schedule):List[GamePrediction] = {
    logger.info(s"Generating predictions for $d.  Have ${gs.size} games")
    val xs = values(values.keys.filter(_.isBefore(d)).maxBy(_.toEpochDay))
    val predictions = gs.flatMap(g => {
      for {hx <- xs.get(g.homeTeamId)
           ax <- xs.get(g.awayTeamId)
      } yield {
        if (hx.value > ax.value) {
          GamePrediction(0L, g.id, key, Some(g.homeTeamId), None, Some(math.round((ax.value - hx.value) * 2.0) / 2.0), None)
        } else {
          GamePrediction(0L, g.id, key, Some(g.awayTeamId), None, Some(math.round((hx.value - ax.value) * 2.0) / 2.0), None)
        }
      }
    })

    predictions

  }


}
