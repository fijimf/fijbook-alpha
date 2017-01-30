package com.fijimf.deepfij.stats.predictor

import com.fijimf.deepfij.models.{Game, Schedule, Team}

trait GamePredictor {
   def sch:Schedule

}


trait WinnerPredictor extends GamePredictor {
  def winner(g:Game): Option[Team]
}

trait SpreadPredictor extends WinnerPredictor {
  def spread(g: Game): Option[Double]

  def winner(g: Game) = {
    spread(g) match {
      case Some(x) if x > 0 => Some(sch.teamsMap(g.homeTeamId))
      case Some(x) if x < 0 => Some(sch.teamsMap(g.awayTeamId))
      case _ => None
    }
  }
}

trait OverUnderPredictor {
  def overUnder(g:Game): Option[Double]
}

trait ScorePredictor extends SpreadPredictor with OverUnderPredictor {
  def scores(s: Game): Option[(Double, Double)]

  def spread(s: Game) = {
    scores(s).map(t => t._1 - t._2)
  }

  def overUnder(s: Game) = {
    scores(s).map(t => t._1 + t._2)
  }
}

trait ProbabilityPredictor {
  def probabilityHome(s: Game): Option[Double]

  def probabilityAway(s: Game) = probabilityHome(s).map(1 - _)
}

