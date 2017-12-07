package com.fijimf.deepfij.stats.predictor

import com.fijimf.deepfij.models.Game

case class LogisticResultLine(game: Game, homeScore: Int, awayScore: Int, homePct: Double, awayPct: Double) {
  def correct: Boolean = (homePct > awayPct && homeScore > awayScore) || (homePct < awayPct && homeScore < awayScore)

  def logLikelihood: Double = if (correct) {
    -math.log(math.max(homePct, awayPct)) / math.log(2.0)
  } else {
    -math.log(math.min(homePct, awayPct)) / math.log(2.0)
  }

  def homeOdds: Double = homePct / awayPct

  def awayOdds: Double = awayPct / homePct
}
