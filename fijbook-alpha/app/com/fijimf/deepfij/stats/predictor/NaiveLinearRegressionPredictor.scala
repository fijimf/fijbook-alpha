package com.fijimf.deepfij.stats.predictor

import java.time.LocalDate

import com.fijimf.deepfij.models.{Game, GamePrediction, Schedule, ScheduleDAO}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class NaiveLinearRegressionPredictor(dao:ScheduleDAO) {

  val key = "naive-regression"

  def predictDate(gs:List[Game], d: LocalDate, sch: Schedule): Future[List[GamePrediction]] = {
    dao.loadStatValues("least-squares", "x-margin-no-ties").map(sv => {
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


  def predictAndSaveDate(d: LocalDate, sch: Schedule): Future[List[Int]] = {
    val games = sch.games.filter(_.date == d)
    val predictions: Future[List[GamePrediction]] = dao.loadGamePredictions(games, key).flatMap(ops => {
      predictDate(games, d, sch).map(nps => {
        val idMap = ops.map(op => op.gameId -> op.id).toMap
        nps.map((np: GamePrediction) => np.copy(id = idMap.getOrElse(np.gameId, 0L)))
      })
    })
    predictions.flatMap(dao.saveGamePredictions)
  }


}
