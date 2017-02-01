package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate
import com.fijimf.deepfij.models.{Game, GamePrediction, Schedule, ScheduleDAO}
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class NaiveLinearRegressionPredictor(dao:ScheduleDAO) extends SchedulePredictor {

  override val key = "naive-regression"

  override def predictDate(gs:List[Game], d: LocalDate, sch: Schedule): Future[List[GamePrediction]] = {
    logger.info(s"Generating predictions for $d.  Have ${gs.size} games")
    dao.loadStatValues("x-margin-no-ties","least-squares").map(sv => {
      logger.info(s"Loaded ${sv.size} model values")
      val values = sv.groupBy(_.date).mapValues(_.map(s => s.teamID -> s).toMap)
      val xs = values(values.keys.filter(_.isBefore(d)).maxBy(_.toEpochDay))
      gs.flatMap(g => {
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
    })
  }


}
