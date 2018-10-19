package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models.dao.schedule.ScheduleDAO
import com.fijimf.deepfij.models.{GamePrediction, Schedule}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

final case class LinearRegressionPredictor(dao: ScheduleDAO) extends SchedulePredictor {

  override val key = "naive-regression"

  val values = Await.result(dao.loadStatValues("x-margin-no-ties", "least-squares").map(sv => {
    logger.info(s"Loaded ${sv.size} model values")
    sv.groupBy(_.date).mapValues(_.map(s => s.teamID -> s).toMap)
  }), Duration.Inf)

  override def predictDate(sch: Schedule, d: LocalDate): List[GamePrediction] = {
    val xs = values(values.keys.filter(_.isBefore(d)).maxBy(_.toEpochDay))
    gamesForDate(sch, d).flatMap(g => {
      for {hx <- xs.get(g._1.homeTeamId)
           ax <- xs.get(g._1.awayTeamId)
      } yield {
        if (hx.value > ax.value) {
          GamePrediction(0L, g._1.id, key, Some(g._1.homeTeamId), None, Some(math.round((ax.value - hx.value) * 2.0) / 2.0), None)
        } else {
          GamePrediction(0L, g._1.id, key, Some(g._1.awayTeamId), None, Some(math.round((hx.value - ax.value) * 2.0) / 2.0), None)
        }
      }
    })
  }

  override def predictTeam(sch: Schedule, key: String): List[GamePrediction] = {
    val xs = values(values.keys.maxBy(_.toEpochDay))
    gamesForTeam(sch, key).flatMap(g => {
      for {hx <- xs.get(g._1.homeTeamId)
           ax <- xs.get(g._1.awayTeamId)
      } yield {
        if (hx.value > ax.value) {
          GamePrediction(0L, g._1.id, key, Some(g._1.homeTeamId), None, Some(math.round((ax.value - hx.value) * 2.0) / 2.0), None)
        } else {
          GamePrediction(0L, g._1.id, key, Some(g._1.awayTeamId), None, Some(math.round((hx.value - ax.value) * 2.0) / 2.0), None)
        }
      }
    })
  }
}
